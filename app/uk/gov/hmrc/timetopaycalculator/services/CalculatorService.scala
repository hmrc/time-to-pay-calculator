/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.Configuration
import play.api.Logger._
import timetopaycalculator.cor.model.{CalculatorInput, DebitInput, Instalment, PaymentSchedule}
import uk.gov.hmrc.timetopaycalculator.models._

import scala.math.BigDecimal.RoundingMode.HALF_UP

/**
 * When calculating interest, the calculation is split up into three sections:
 * 1. Historic Interest - Interest due on debits in the past up to but not including the start date.
 * 2. Initial Payment Interest - Interest due on debits over the next 7 days, when an initial payment is
 * made. This takes into account that the initial payment is matched against the oldest debits first
 * 3. Instalment amounts and interest - The amount paid in each instalment as well as the interest
 * generated on liable debits up to the final payment. This is from the start date of the calculation
 * or start date + 7 days if an initial payment was made.
 */
@Singleton
class CalculatorService @Inject() (val interestService: InterestRateService, configuration: Configuration)(val durationService: DurationService) {

  val DebitDueAndCalculationDatesWithinRate = Tuple2(true, true)
  val DebitDueDateWithinRate = Tuple2(true, false)
  val CalculationDateWithinRate = Tuple2(false, true)

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  /**
   * Build a PaymentSchedule, calculated in parts - historic interest (in the past up to start date),
   * initial payment interest, future interest (start date going forward) and instalments (monthly payments).
   */
  def buildSchedule(implicit calculation: CalculatorInput): PaymentSchedule = {
    // Builds a seq of seq debits, where each sub seq of debits is built from the different interest rate boundaries a debit crosses
    val overallDebits: Seq[Seq[Debit]] = calculation.debits.filter(_.dueDate.isBefore(calculation.startDate)).map {
      processDebit
    }

    // Calculate interest on old debits that have incurred interest up to the point of the current calculation date (today)
    val totalHistoricInterest = (for {
      debit <- overallDebits.map(_.filterNot(_.dueDate.isAfter(calculation.startDate)))
    } yield calculateHistoricInterest(debit)).sum

    // Calculate interest for the first 7 days until the initial payment is actually taken out of the taxpayer's account
    val initialPaymentInterest = if (calculation.initialPayment > 0)
      calculateInitialPaymentInterest(calculation.debits.filter(_.dueDate.isBefore(calculation.startDate.plusWeeks(1))))
    else BigDecimal(0)

    // Calculate the schedule of regular payments on the all debits due before endDate
    val instalments = calculateStagedPayments

    // Total amount of debt without interest
    val amountToPay = calculation.debits.map(_.amount).sum

    val totalInterest = (instalments.map(_.interest).sum + totalHistoricInterest + initialPaymentInterest).setScale(2, HALF_UP)

    PaymentSchedule(
      calculation.startDate,
      calculation.endDate,
      calculation.initialPayment,
      amountToPay,
      amountToPay - calculation.initialPayment,
      totalInterest,
      amountToPay + totalInterest,
      instalments.init :+ Instalment(
        instalments.last.paymentDate,
        instalments.last.amount + totalInterest,
        instalments.last.interest
      )
    )
  }

  /**
   * Calculate instalments including interest charged on each instalment, while taking into account
   * interest is not charged on debits where initial payment fully or partially clears the oldest debits or
   * if the debit is not liable for interest (due in the future after the end date).
   */
  def calculateStagedPayments(implicit calculation: CalculatorInput): Seq[Instalment] = {
    // Get the dates of each instalment payment

    val trueFirstPaymentDate = calculation.firstPaymentDate.getOrElse(calculation.startDate)
    val repayments = durationService.getRepaymentDates(trueFirstPaymentDate, calculation.endDate)
    val numberOfPayments = BigDecimal(repayments.size)

    //This var was already there in CalculatorInput case class ... TODO: refactor it without this var
    var initialPaymentRemaining: BigDecimal = calculation.initialPayment
      def applyInitialPaymentToDebt(debtAmount: BigDecimal): BigDecimal = debtAmount match {
        case amt if amt <= initialPaymentRemaining =>
          initialPaymentRemaining = initialPaymentRemaining - debtAmount; 0
        case amt => val remainingDebt = amt - initialPaymentRemaining; initialPaymentRemaining = 0; remainingDebt
      }

    val instalments = calculation.debits.sortBy(_.dueDate).flatMap { debit =>
      // Check if initial payment has been cleared - if not, then date to calculate interest from is a week later
      val calculateFrom = if (initialPaymentRemaining > 0)
        calculation.startDate.plusWeeks(1) else calculation.startDate

      val calculationDate = if (calculateFrom.isBefore(debit.dueDate))
        debit.dueDate else calculateFrom

      // Subtract the initial payment amount from the debts, beginning with the oldest
      val principal = applyInitialPaymentToDebt(debit.amount)

      val monthlyCapitalRepayment = (principal / numberOfPayments).setScale(2, HALF_UP)

      val currentInterestRate = interestService.rateOn(calculationDate).rate
      val currentDailyRate = currentInterestRate / BigDecimal(Year.of(calculationDate.getYear).length()) / BigDecimal(100)

      repayments.map { r =>
        val daysInterestToCharge = BigDecimal(durationService.getDaysBetween(calculationDate, r.plusDays(1)))

        val interest = monthlyCapitalRepayment * currentDailyRate * daysInterestToCharge

        val ins = Instalment(r, monthlyCapitalRepayment, interest)
        logger.info(s"Repayment $monthlyCapitalRepayment ($calculationDate - $r) $daysInterestToCharge @ $currentDailyRate = $interest")
        ins
      }
    }

    // Combine instalments that are on the same day
    repayments.map { x =>
      instalments.filter(_.paymentDate.isEqual(x)).reduce((z, y) => Instalment(z.paymentDate, z.amount + y.amount, z.interest + y.interest))
    }
  }

  /**
   * Calculate interest for the initial payment amount for the first 7 days until the initial payment is taken out of the taxpayer's account.
   *
   * @param debits - only debits that are not after calculation date plus a week
   */

  def calculateInitialPaymentInterest(debits: Seq[DebitInput])(implicit calculation: CalculatorInput): BigDecimal = {
    val currentInterestRate = interestService.rateOn(calculation.startDate).rate
    val currentDailyRate = currentInterestRate / BigDecimal(Year.of(calculation.startDate.getYear).length()) / BigDecimal(100)

    val sortedDebits: Seq[DebitInput] = debits.sortBy(_.dueDate)

      def processDebits(amount: BigDecimal, debits: Seq[DebitInput]): BigDecimal = {
        debits match {
          case debit :: Nil => calculateAmount(amount, debit)._1 * calculateDays(debit) * currentDailyRate
          case debit :: remaining =>
            val result = calculateAmount(amount, debit)
            processDebits(result._2, remaining) + (result._1 * calculateDays(debit) * currentDailyRate)
          case Nil => 0
        }
      }

      def calculateDays(debit: DebitInput): Long = {
        if (debit.dueDate.isBefore(calculation.startDate))
          configuration.getOptional[Long]("defaultInitialPaymentDays").getOrElse(7)
        else
          durationService.getDaysBetween(debit.dueDate, calculation.startDate.plusWeeks(1))
      }

      // Return - amount (used in the calculation), remaining downPayment
      def calculateAmount(amount: BigDecimal, debit: DebitInput): (BigDecimal, BigDecimal) = {
        if (amount > debit.amount) {
          (debit.amount, amount - debit.amount)
        } else {
          (amount, 0)
        }
      }

    val initPaymentInterest = processDebits(calculation.initialPayment, sortedDebits)
    logger.info(s"InitialPayment Interest: $initPaymentInterest")
    initPaymentInterest
  }

  /**
   * Get the historic interest rates that should be applied to a given debit and split the debit
   * into multiple debits, covering each interest rate.
   */
  private def processDebit(debit: DebitInput)(implicit calculation: CalculatorInput): Seq[Debit] = {
    interestService.getRatesForPeriod(
      debit.dueDate,
      calculation.endDate
    ).map { rate =>
        (rate.containsDate(debit.dueDate), rate.containsDate(calculation.endDate)) match {
          case DebitDueAndCalculationDatesWithinRate => Debit(
            amount  = debit.amount,
            dueDate = debit.dueDate,
            endDate = calculation.endDate,
            rate    = rate
          )
          case DebitDueDateWithinRate => Debit(
            amount  = debit.amount,
            dueDate = debit.dueDate,
            endDate = rate.endDate,
            rate    = rate
          )
          case CalculationDateWithinRate => Debit(
            amount  = debit.amount,
            dueDate = rate.startDate,
            endDate = calculation.endDate,
            rate    = rate
          )
          case _ => Debit(
            amount  = debit.amount,
            dueDate = rate.startDate,
            endDate = rate.endDate,
            rate    = rate
          )
        }
      }
  }

  /**
   * Calculate the amount of historic interest on liable debits, taking into account whether
   * the number of days between two dates is inclusive (count one of the dates) or double
   * inclusive (count both days).
   */
  private def calculateHistoricInterest(debits: Seq[Debit])(implicit calculation: CalculatorInput): BigDecimal = {
    debits.map { debit =>
      val debitRateEndDate = debit.rate.endDate
      val inclusive = if (!(debits.head.equals(debit) | debits.last.equals(debit))) 1 else 0
      val endDate = historicRateEndDate(debitRateEndDate)

      val numberOfDays = durationService.getDaysBetween(debit.dueDate, endDate) + inclusive
      val historicRate = debit.historicDailyRate
      val total = historicRate * debit.amount * numberOfDays

      logger.info(s"Historic interest: rate $historicRate days $numberOfDays amount ${debit.amount} total = $total")
      logger.info(s"Debit due date: ${debit.dueDate} and end date: $endDate is inclusive: $inclusive")
      logger.info(s"Debit Rate date: $debitRateEndDate and calculation start date: ${calculation.startDate}")
      total
    }.sum
  }

  def historicRateEndDate(debitEndDate: LocalDate)(implicit calculation: CalculatorInput): LocalDate =
    if (debitEndDate.getYear.equals(calculation.startDate.getYear)) calculation.startDate else debitEndDate
}
