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

import scala.math.BigDecimal.RoundingMode.{FLOOR, HALF_UP}

class CalculatorService(interestService: InterestRateService, durationService: DurationService) {
  val debitDueAndCalculationDatesWithinRate = (true, true)
  val debitDueDateWithinRate = (true, false)
  val calculationDateWithinRate = (false, true)

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  def generateMultipleSchedules(implicit calculation: Calculation) = Seq(buildSchedule).map { s => logger.info(s"Payment Schedule: $s"); s }

  def calculateStagedPayments(overalDebit: Debit)(implicit calculation: Calculation): Seq[Instalment] = {
    val principal = calculation.applyInitialPaymentToDebt(overalDebit.amount)
    val repayments = durationService.getRepaymentDates(calculation.getFirstPaymentDate, calculation.endDate)
    val numberOfPayments = BigDecimal(repayments.size)
    val monthlyCapitalRepayment = (principal / numberOfPayments).setScale(2, HALF_UP)

    repayments.map { r =>
      val daysInterestToCharge = BigDecimal(durationService.getDaysBetween(calculation.startDate, r))
      val interest = (monthlyCapitalRepayment * daysInterestToCharge * overalDebit.rate.map(_.dailyRate).getOrElse(BigDecimal(0)) / BigDecimal(100)).setScale(2, FLOOR)
      val ins = Instalment(r, monthlyCapitalRepayment, interest)
      logger.info(s"Repayment $monthlyCapitalRepayment (${calculation.startDate} - $r) $daysInterestToCharge @ ${overalDebit.rate.map(_.rate).get.setScale(2, HALF_UP)} = $interest")
      ins
    }
  }

  def buildSchedule(implicit calculation: Calculation): PaymentSchedule = {
    val overalDebit = calculation.debits.map {
      processDebit
    }.reduce(combine(differingAmounts))

    val instalments = calculateStagedPayments(overalDebit)

    val amountToPay = calculation.debits.map(_.amount).sum
    val totalInterest = overalDebit.interest.amountAccrued + instalments.map(_.interest).sum

    PaymentSchedule(calculation.startDate, calculation.endDate, calculation.initialPayment, amountToPay,
      amountToPay - calculation.initialPayment, totalInterest, amountToPay + totalInterest,
      instalments.init :+ Instalment(instalments.last.paymentDate, instalments.last.amount + totalInterest, instalments.last.interest))
  }

  private def processDebit(implicit calculation: Calculation): (Debit) => Debit = { debit =>
    interestService.getRatesForPeriod(debit.dueDate, calculation.endDate).map { rate =>
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
    }.reduce(combine(sameAmounts))
  }

  private def differingAmounts = (d1: Debit, d2: Debit) => d1.amount + d2.amount

  private def sameAmounts = (d1: Debit, d2: Debit) => d1.amount

  private def combine(sumAmounts: (Debit, Debit) => BigDecimal)(implicit calculation: Calculation) = { (d1: Debit, d2: Debit) =>
    val interest = flatInterest.apply(d2)
    val rate1 = d1.rate.map(_.rate).getOrElse(BigDecimal(0))
    val rate2 = d2.rate.map(_.rate).getOrElse(BigDecimal(0))

    val fractionOfYrAtRate1 = BigDecimal(durationService.getDaysBetween(d1.dueDate, d2.dueDate.minusDays(1))) / BigDecimal(d1.dueDate.lengthOfYear())
    val fractionOfYrAtRate2 = BigDecimal(durationService.getDaysBetween(d2.dueDate, d2.endDate.getOrElse(d2.dueDate))) / BigDecimal(d2.dueDate.lengthOfYear())

    val rate = ((rate1 * fractionOfYrAtRate1) + (rate2 * fractionOfYrAtRate2)) / (fractionOfYrAtRate1 + fractionOfYrAtRate2)
    val combinedRate = InterestRate(d1.dueDate, d2.endDate, rate)
    Debit(d1.originCode, sumAmounts(d1, d2), Interest(d1.interest.amountAccrued + interest, calculation.startDate), d1.dueDate, d2.endDate, Some(combinedRate))
  }

  private def flatInterest(implicit calculation: Calculation): (Debit) => BigDecimal = { l =>
    val startDate = Seq(l.dueDate, l.interest.calculationDate).max
    val endDate = Seq(calculation.startDate.minusDays(1), l.endDate.getOrElse(calculation.startDate)).min

    if (startDate.isAfter(endDate)) {
      BigDecimal(0)
    } else {
      val numberOfDays = BigDecimal(durationService.getDaysBetween(startDate, endDate))
      val rate = l.rate.map(_.rate).getOrElse(BigDecimal(0))
      val fractionOfYear = numberOfDays / BigDecimal(l.dueDate.lengthOfYear())
      val interestToPay = (l.amount * rate * fractionOfYear / BigDecimal(100)).setScale(2, HALF_UP)

      logger.info(s"Debit: £${l.amount}\t$startDate\t-\t$endDate\t@\t$rate\tover\t$numberOfDays\tdays =\t£$interestToPay (simple)")

      interestToPay
    }
  }
}
