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

import play.api.Logger._
import play.api.Play.{configuration, current}
import uk.gov.hmrc.timetopaycalculator.models._

import scala.math.BigDecimal.RoundingMode.HALF_UP

class CalculatorService(interestService: InterestRateService, durationService: DurationService) {
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
      val currentInterestRate = InterestRateService.rateOn(calculationDate).getOrElse(InterestRate.NONE).rate
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

  /**
    * Calculate interest for the initial payment amount for the first 7 days until it (the initial payment) is taken out of the taxpayer's account
    *
    * @param debits - only debits that are not after calculation date plus a week
    * @param calculation
    * @return
    */
  def calculateInitialPaymentInterest(debits: Seq[Debit])(implicit calculation: Calculation): BigDecimal = {
    val currentInterestRate = InterestRateService.rateOn(calculation.startDate).getOrElse(InterestRate.NONE).rate
    val currentDailyRate = currentInterestRate / BigDecimal(Year.of(calculation.startDate.getYear).length()) / BigDecimal(100)

    var downPayment = calculation.initialPayment

    debits.sortBy(_.dueDate).map {
      debit =>
        // you can replace the below with a recursive function
        val amount =
          // if the downpayment is greater than the amount, [charge up to 7 days interest on] the total amount of the debit
          if (downPayment > debit.amount) {
            // carry leftover downpayment across remaining debts
            downPayment -= debit.amount
            debit.amount
        } else {
          // charge interest on the [leftover] downpayment amount, clear downpayment as it is now exhausted
          val toReturn = downPayment
          downPayment = 0
          toReturn
        }

        val daysOfInterest = if(debit.dueDate.isBefore(calculation.startDate))
          configuration.getInt("defaultInitialPaymentDays").getOrElse(7)
        else
          durationService.getDaysBetween(debit.dueDate, calculation.startDate.plusWeeks(1))

        val interest = daysOfInterest * currentDailyRate * amount
        logger.info(s"Initial payment interest of $interest at $daysOfInterest days at rate $currentDailyRate")
        interest
    }.sum
  }

//  private def calculateInitialInterest(debit: Debit, amount: BigDecimal): BigDecimal = {
//
//  }
//  private def calculateDownPayment(debit: Debit)(implicit calculation: Calculation): BigDecimal = {
//    val downPayment = calculation.initialPayment
//
//    if (downPayment > debit.amount) {
//      downPayment = downPayment - debit.amount
//      debit.amount
//    } else {
//      val toReturn = downPayment
//      downPayment = 0
//      toReturn
//    }
//  }
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

    // calculate the schedule of regular payments on the overall amount
    val instalments = calculateStagedPayments(overallDebits)

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