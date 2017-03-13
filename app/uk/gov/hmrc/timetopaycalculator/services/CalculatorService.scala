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
import javax.inject.{Inject, Singleton}

import play.api.Logger._
import play.api.Play.{configuration, current}
import uk.gov.hmrc.timetopaycalculator.models._

import scala.math.BigDecimal.RoundingMode.HALF_UP

@Singleton
class CalculatorService @Inject() (val interestService: InterestRateService) (val durationService: DurationService) {
  object DebitDueAndCalculationDatesWithinRate extends Tuple2(true, true)
  object DebitDueDateWithinRate extends Tuple2(true, false)
  object CalculationDateWithinRate extends Tuple2(false, true)

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  def generateMultipleSchedules(implicit calculation: Calculation):Seq[PaymentSchedule] =
    Seq(buildSchedule).map { s => logger.info(s"Payment Schedule: $s"); s }

  /**
    * Calculate instalments including interest charged on each instalment, while taking into account
    * interest is not charged on debts where initial payment fully or partially clears the oldest debts or
    * if the debt is not liable for interest (due in the future after the end date)
    *
    * @param calculation
    * @return
    */
  def calculateStagedPayments(implicit calculation: Calculation): Seq[Instalment] = {
    //get the dates of each instalment payment
    val repayments = durationService.getRepaymentDates(calculation.getFirstPaymentDate, calculation.endDate)
    val numberOfPayments = BigDecimal(repayments.size)

    val instalments = calculation.debits.sortBy(_.dueDate).flatMap { debit =>
      //subtract the initial payment amount from the debts, beginning with the oldest
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

    // combine instalments that are on the same day
    repayments.map { x =>
      instalments.filter(_.paymentDate.isEqual(x)).reduce( (z, y) => Instalment(z.paymentDate, z.amount + y.amount, z.interest + y.interest))
    }
  }

  /**
    * Calculate interest for the initial payment amount for the first 7 days until it (the initial payment) is taken out of the taxpayer's account
    *
    * @param debits - only debits that are not after calculation date plus a week
    * @param calculation
    * @return
    */
  def calculateInitialPaymentInterest(debits: Seq[Debit])(implicit calculation: Calculation): BigDecimal = {
    val currentInterestRate = interestService.rateOn(calculation.startDate).getOrElse(InterestRate.NONE).rate
    val currentDailyRate = currentInterestRate / BigDecimal(Year.of(calculation.startDate.getYear).length()) / BigDecimal(100)

    val sortedDebits: Seq[Debit] = debits.sortBy(_.dueDate)

    def processDebits(amount: BigDecimal, debits: Seq[Debit]): BigDecimal = {
      debits match {
        case debit :: remaining =>
          val result = calculateAmount(amount, debit)
          processDebits(result._2, remaining) + (result._1 * calculateDays(debit) * currentDailyRate)
        case debit :: Nil => calculateAmount(amount, debit)._1 * calculateDays(debit) * currentDailyRate
        case Nil => 0
      }
    }

    def calculateDays(debit: Debit): Long = {
      if(debit.dueDate.isBefore(calculation.startDate))
        configuration.getLong("defaultInitialPaymentDays").getOrElse(7)
      else
        durationService.getDaysBetween(debit.dueDate, calculation.startDate.plusWeeks(1))
    }

    //return - amount (used in the calculation), remaining downPayment
    def calculateAmount(amount: BigDecimal, debit: Debit): (BigDecimal, BigDecimal) = {
      if(amount > debit.amount) {
        (debit.amount, amount - debit.amount)
      } else {
        (amount, 0)
      }
    }

    processDebits(calculation.initialPayment, sortedDebits)
  }

  /**
    * Build a PaymentSchedule, calculated in parts - historic interest (in the past up to start date),
    * initial payment interest, future interest (start date going forward) and instalments (monthly payments).
    *
    * @param calculation
    * @return
    */
  def buildSchedule(implicit calculation: Calculation): PaymentSchedule = {
    // set interest dates on the debits per year
    val overallDebits = calculation.debits.filter(_.dueDate.isBefore(calculation.startDate)).flatMap {
      processDebit
    }

    // calculate interest on old debts that have incurred interest up to the point of the current calculation date (now)
    val totalHistoricInterest = (for {
      debit <- overallDebits.filterNot(_.dueDate.isAfter(calculation.startDate))
    } yield calculateHistoricInterest(debit)).sum

    // calculate interest for the first 7 days until the initial payment is actually taken out of the taxpayer's account
    val initialPaymentInterest = if(calculation.initialPayment > 0)
      calculateInitialPaymentInterest(calculation.debits.filter(_.dueDate.isBefore(calculation.startDate.plusWeeks(1))))
    else BigDecimal(0)

    // calculate the schedule of regular payments on the all debits due before endDate
    val instalments = calculateStagedPayments

    // total amount of debt without interest
    val amountToPay = calculation.debits.map(_.amount).sum

    val totalInterest = (instalments.map(_.interest).sum + totalHistoricInterest + initialPaymentInterest).setScale(2, HALF_UP)

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