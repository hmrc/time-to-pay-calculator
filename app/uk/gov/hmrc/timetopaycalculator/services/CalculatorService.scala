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

import java.time.{LocalDate, Year}
import javax.inject.Inject

import play.api.Logger._
import uk.gov.hmrc.timetopaycalculator.models._

import scala.math.BigDecimal.RoundingMode.HALF_UP

class CalculatorService @Inject() (val interestService: InterestRateService) (val durationService: DurationService) {
  object DebitDueAndCalculationDatesWithinRate extends Tuple2(true, true)
  object DebitDueDateWithinRate extends Tuple2(true, false)
  object CalculationDateWithinRate extends Tuple2(false, true)

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  def generateMultipleSchedules(implicit calculation: Calculation):Seq[PaymentSchedule] =
    Seq(buildSchedule).map { s => logger.info(s"Payment Schedule: $s"); s }

  def calculateStagedPayments(overallDebits: Seq[Debit])(implicit calculation: Calculation): Seq[Instalment] = {
    val repayments = durationService.getRepaymentDates(calculation.getFirstPaymentDate, calculation.endDate)
    val numberOfPayments = BigDecimal(repayments.size)

    val instalments = calculation.debits.flatMap { debit =>
      val principal = calculation.applyInitialPaymentToDebt(debit.amount)
      val monthlyCapitalRepayment = (principal / numberOfPayments).setScale(2, HALF_UP)
      val calculationDate = if (calculation.startDate.isBefore(debit.dueDate)) debit.dueDate else calculation.startDate
      val currentInterestRate = interestService.rateOn(calculationDate).getOrElse(InterestRate.NONE).rate
      val currentDailyRate = currentInterestRate / BigDecimal(Year.of(calculationDate.getYear).length()) / BigDecimal(100)

      repayments.map { r =>
        val daysInterestToCharge = BigDecimal(durationService.getDaysBetween(calculationDate, r))

        val interest = monthlyCapitalRepayment  * currentDailyRate * daysInterestToCharge

        val ins = Instalment(r, monthlyCapitalRepayment, interest)
        logger.info(s"Repayment $monthlyCapitalRepayment ($calculationDate - $r) $daysInterestToCharge @ $currentDailyRate = $interest")
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

    val totalInterest =  (instalments.map(_.interest).sum + totalHistocInterest).setScale(2, HALF_UP)

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

  def historicRateDaysInclusive(debitEndDate: LocalDate)(implicit calculation: Calculation): Boolean =
    if(debitEndDate.getYear.equals(calculation.startDate.getYear)) false else true

  def historicRateEndDate(debitEndDate: LocalDate)(implicit calculation: Calculation): LocalDate =
    if(debitEndDate.getYear.equals(calculation.startDate.getYear)) calculation.startDate else debitEndDate

  private def calculateHistoricInterest(debit: Debit)(implicit calculation: Calculation): BigDecimal = {
    val debitEndDate = debit.rate.getOrElse(InterestRate.NONE).endDate.get
    val inclusive = historicRateDaysInclusive(debitEndDate) // true except the extra case of current year
    val endDate = historicRateEndDate(debitEndDate)

    val numberOfDays = durationService.getDaysBetween(debit.dueDate, endDate, inclusive)
    val historicRate = debit.historicDailyRate
    val total = historicRate*debit.amount*numberOfDays

    logger.info(s"Historic interest: rate $historicRate days $numberOfDays  amount ${debit.amount} total = $total")
    total
  }
}