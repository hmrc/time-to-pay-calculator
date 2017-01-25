/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.timetopaycalculator.services

import java.time.LocalDate
import java.time.Year

import play.api.Logger._
import uk.gov.hmrc.timetopaycalculator.models._

import scala.math.BigDecimal.RoundingMode.{FLOOR, HALF_UP}

class CalculatorService(interestService: InterestRateService, durationService: DurationService) {
  object DebitDueAndCalculationDatesWithinRate extends Tuple2(true, true)
  object DebitDueDateWithinRate extends Tuple2(true, false)
  object CalculationDateWithinRate extends Tuple2(false, true)

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  def generateMultipleSchedules(implicit calculation: Calculation) = Seq(buildSchedule).map { s => logger.info(s"Payment Schedule: $s"); s }

  def calculateStagedPayments(overallDebits: Seq[Debit])(implicit calculation: Calculation): Seq[Instalment] = {
    val repayments = durationService.getRepaymentDates(calculation.getFirstPaymentDate, calculation.endDate)
    val numberOfPayments = BigDecimal(repayments.size)

    val instalments = calculation.debits.flatMap { debit =>

      val principal = calculation.applyInitialPaymentToDebt(debit.amount)
      val monthlyCapitalRepayment = (principal / numberOfPayments).setScale(2, HALF_UP)
      val calculationDate = if (calculation.startDate.isBefore(debit.dueDate)) debit.dueDate else calculation.startDate

      val currentDailyRate = InterestRateService.rateOn(calculationDate).getOrElse(InterestRate.NONE).rate / BigDecimal(Year.of(calculationDate.getYear).length()) / BigDecimal(100)

      repayments.map { r =>
        val daysInterestToCharge = BigDecimal(durationService.getDaysBetween(calculationDate, r))

        val interest = monthlyCapitalRepayment  * currentDailyRate * daysInterestToCharge

        val ins = Instalment(r, monthlyCapitalRepayment, interest)
        //logger.info(s"Repayment $monthlyCapitalRepayment ($calculationDate - $r) $daysInterestToCharge @ ${overallDebit.rate.map(_.rate).get.setScale(2, HALF_UP)} = $interest")
        ins
      }
    }

    repayments.map { x =>
      instalments.filter(_.paymentDate.isEqual(x)).reduce( (z, y) => Instalment(z.paymentDate, z.amount + y.amount, z.interest + y.interest))
    }
  }

  def buildSchedule(implicit calculation: Calculation): PaymentSchedule = {
    val overallDebits = calculation.debits.filter(_.dueDate.isBefore(calculation.startDate)).flatMap {
      processDebit
    }

    val totalHistocInterest = (for {
      debit <- overallDebits
    } yield calculateHistoricInterest(debit)).sum

    val instalments = calculateStagedPayments(overallDebits)

    val amountToPay = calculation.debits.map(_.amount).sum

    val totalInterest =  instalments.map(_.interest).sum + totalHistocInterest

    PaymentSchedule(calculation.startDate, calculation.endDate, calculation.initialPayment, amountToPay,
      amountToPay - calculation.initialPayment, totalInterest, amountToPay + totalInterest,
      instalments.init :+ Instalment(instalments.last.paymentDate, instalments.last.amount + totalInterest, instalments.last.interest))
  }

  private def processDebit(implicit calculation: Calculation): (Debit) => Seq[Debit] = { debit =>
    interestService.getRatesForPeriod(debit.dueDate, calculation.endDate).map { rate =>
      (rate.containsDate(debit.dueDate), rate.containsDate(calculation.endDate)) match {
        case DebitDueAndCalculationDatesWithinRate => debit.copy(endDate = Option(calculation.endDate), rate = Option(rate))
        case DebitDueDateWithinRate =>                debit.copy(endDate = rate.endDate, rate = Option(rate))
        case CalculationDateWithinRate =>             debit.copy(dueDate = rate.startDate, endDate = Option(calculation.endDate), rate = Option(rate))
        case _ =>                                     debit.copy(dueDate = rate.startDate, endDate = rate.endDate, rate = Option(rate))
      }
    }
  }

  private def calculateHistoricInterest(debit: Debit): BigDecimal = {
    // call duration service to work out the number of days
    debit.historicDailyRate*debit.amount*189 // TODO replace 189 with num days
  }

  private def flatInterest(implicit calculation: Calculation): (Debit) => BigDecimal = { l =>
    val startDate = Seq(l.dueDate, l.interest.getOrElse(Interest(BigDecimal(0), l.dueDate)).calculationDate).max
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