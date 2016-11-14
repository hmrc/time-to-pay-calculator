/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.selfservicetimetopay.services

import java.time.LocalDate

import play.api.Logger._
import uk.gov.hmrc.selfservicetimetopay.models._

import scala.math.BigDecimal.{RoundingMode, valueOf}
import scala.math.BigDecimal.RoundingMode.{FLOOR, HALF_UP}
import scala.util.Try

class CalculatorService(interestService: InterestRateService, durationService: DurationService) {
  val ONE_HUNDRED = 100
  val MONTHS_IN_YEAR = 12
  val PRECISION_2DP = 2
  val PRECISION_10DP = 10

  val debitDueAndCalculationDatesWithinRate = (true, true)
  val debitDueDateWithinRate = (true, false)
  val calculationDateWithinRate = (false, true)

  private def processDebit(calculation: Calculation): (Debit) => Debit = { debit =>
    val debits = interestService.getRatesForPeriod(debit.dueDate, calculation.endDate).map { rate =>
      (rate.containsDate(debit.dueDate), rate.containsDate(calculation.endDate)) match {
        case `debitDueAndCalculationDatesWithinRate` =>
          Debit.partialOf(debit, debit.dueDate, calculation.endDate, rate)
        case `debitDueDateWithinRate` =>
          Debit.partialOf(debit, debit.dueDate, rate.endDate.get, rate)
        case `calculationDateWithinRate` =>
          Debit.partialOf(debit, rate.startDate, calculation.endDate, rate)
        case _ =>
          Debit.partialOf(debit, rate.startDate, rate.endDate.get, rate)
      }
    }

    debits.tail.fold(debits.head) { combine(calculation) }
  }

  private def combine(calculation: Calculation) = { (d1: Debit, d2: Debit) =>
    val interest = flatInterest(calculation).apply(d2)
    val rate1 = d1.rate.map(_.rate).getOrElse(BigDecimal(0))
    val rate2 = d2.rate.map(_.rate).getOrElse(BigDecimal(0))

    val fractionOfYrAtRate1 = valueOf(durationService.getDaysBetween(d1.dueDate, d2.dueDate.minusDays(1))) / valueOf(d1.dueDate.lengthOfYear())
    val fractionOfYrAtRate2 = valueOf(durationService.getDaysBetween(d2.dueDate, d2.endDate.getOrElse(d2.dueDate))) / valueOf(d2.dueDate.lengthOfYear())

    val rate = ((rate1 * fractionOfYrAtRate1) + (rate2 * fractionOfYrAtRate2)) / (fractionOfYrAtRate1 + fractionOfYrAtRate2)
    val combinedRate = InterestRate(d1.dueDate, d2.endDate, rate)
    Debit(d1.originCode, d1.amount, Interest(d1.interest.amountAccrued + interest, calculation.startDate), d1.dueDate, d2.endDate, Some(combinedRate))
  }

  def buildSchedule(calculation: Calculation): PaymentSchedule = {
    val totalInterest = calculation.debits.map(processDebit(calculation)
      .andThen(amortizedInterest(calculation))).sum.setScale(2, RoundingMode.HALF_UP)

    val amountToPay = calculation.debits.map(_.amount).sum

    val totalForInstalmentPayment = amountToPay - calculation.initialPayment + totalInterest
    val instalmentPaymentDates = durationService.getRepaymentDates(calculation.startDate, calculation.endDate)
    val numberOfInstalments = valueOf(instalmentPaymentDates.size)

    val instalmentPayment = (totalForInstalmentPayment / numberOfInstalments).setScale(2, RoundingMode.HALF_UP)
    val finalPayment = (totalForInstalmentPayment - instalmentPayment * (numberOfInstalments - 1)).setScale(2, RoundingMode.HALF_UP)

    val instalments: Seq[Instalment] = instalmentPaymentDates.map { paymentDate =>
      val instalment = paymentDate match {
        case d if d == instalmentPaymentDates.last => Instalment(d, finalPayment)
        case d => Instalment(d, instalmentPayment)
      }

      logger.info("Instalment: {}", instalment)

      instalment
    }

    PaymentSchedule(calculation.initialPayment, amountToPay, amountToPay - calculation.initialPayment, totalInterest, amountToPay + totalInterest, instalments)
  }

  def generateMultipleSchedules(calculation: Calculation): Seq[PaymentSchedule] = {
    Seq(buildSchedule(calculation))
  }

  private def flatInterest(calculation: Calculation): (Debit) => BigDecimal = { l =>
    val startDate = Seq(l.dueDate, l.interest.calculationDate).max
    val endDate = Seq(calculation.startDate.minusDays(1), l.endDate.getOrElse(calculation.startDate)).min

    if (startDate.isAfter(endDate)) {
      BigDecimal(0)
    } else {
      val numberOfDays = durationService.getDaysBetween(startDate, endDate)
      val rate = l.rate.map(_.rate).getOrElse(BigDecimal(0))
      val fractionOfYear = BigDecimal(numberOfDays) / BigDecimal(l.dueDate.lengthOfYear())
      val interestToPay = (l.amount * rate * fractionOfYear / 100).setScale(2, RoundingMode.HALF_UP)

      logger.info(s"Debit: £${l.amount}\t$startDate\t-\t$endDate\t@\t$rate\tover\t$numberOfDays\tdays =\t£$interestToPay (simple)")

      interestToPay
    }
  }

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  private def amortizedInterest(calculation: Calculation) = { debit: Debit =>
    val startDate = Seq(calculation.startDate, debit.dueDate).max

    val rate = debit.rate.map(_.rate).getOrElse(BigDecimal(0))
    val endDate = debit.endDate.getOrElse(startDate)
    val numberOfDays = durationService.getDaysBetween(startDate, endDate)
    val principal = calculation.applyInitialPaymentToDebt(debit.amount)

    val rateForPeriod = rate / MONTHS_IN_YEAR / 100
    val numberOfPeriods = durationService.getRepaymentDates(startDate, endDate).size

    val numerator = rateForPeriod * (1 + rateForPeriod).pow(numberOfPeriods)
    val denominator = (1 + rateForPeriod).pow(numberOfPeriods) - 1

    val amountPerPeriod = principal * numerator / denominator
    val totalRepayable = (amountPerPeriod * numberOfPeriods).setScale(PRECISION_2DP, HALF_UP)
    val interestToPay = (totalRepayable - principal).setScale(PRECISION_2DP, FLOOR)

    logger.info(s"Debit: £$principal\t$startDate\t-\t$endDate\t@\t$rate\tover\t$numberOfDays\tdays =\t£$interestToPay (amortized), total payable £$totalRepayable (+ ${debit.interest.amountAccrued} pre TTP interest)")

    interestToPay + debit.interest.amountAccrued
  }
}
