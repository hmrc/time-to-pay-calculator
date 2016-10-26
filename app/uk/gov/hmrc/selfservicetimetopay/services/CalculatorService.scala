package uk.gov.hmrc.selfservicetimetopay.services

import java.time.LocalDate

import play.api.Logger._
import uk.gov.hmrc.selfservicetimetopay.models.{Calculation, Instalment, PaymentSchedule}

trait CalculatorService {
  val ONE_HUNDRED = 100
  val MONTHS_IN_YEAR = 12
  val PRECISION_2DP = 2
  val PRECISION_10DP = 10

  val validator: CalculationValidator = ???
  val interestService: InterestRateService = ???
  val durationService = DurationService

  def buildSchedule(calculation: Calculation): PaymentSchedule = {
    val totalInterest: BigDecimal = calculation.liabilities.map(processLiability(calculation)).map(amortizedInterest(calculation)).sum

    val amountToPay: BigDecimal = calculation.liabilities.map(_.amount).sum

    val totalForInstalmentPayment = amountToPay - calculation.initialPayment + totalInterest
    val instalmentPaymentDates: Seq[LocalDate] = durationService.getRepaymentDates(calculation.startDate, calculation.endDate)
    val numberOfInstalments: Int = instalmentPaymentDates.size

    val instalmentPayment: BigDecimal = totalForInstalmentPayment / numberOfInstalments
    val finalPayment: BigDecimal = totalForInstalmentPayment - instalmentPayment * numberOfInstalments - 1

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
    validator.validate(calculation)
    Seq(buildSchedule(calculation))
  }
}
