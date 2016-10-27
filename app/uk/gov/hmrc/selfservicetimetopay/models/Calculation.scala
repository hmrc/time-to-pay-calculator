package uk.gov.hmrc.selfservicetimetopay.models

import java.time.LocalDate

case class Calculation(liabilities: Seq[Liability], initialPayment: BigDecimal, startDate: LocalDate, endDate: LocalDate, paymentFrequency: String) {
  var initialPaymentRemaining: BigDecimal = 0

  def applyInitialPaymentToDebt(debtAmount: BigDecimal): BigDecimal = debtAmount match {
    case amt if amt <= initialPaymentRemaining =>  initialPaymentRemaining = initialPaymentRemaining - debtAmount; 0
    case amt => val remainingDebt = amt - initialPaymentRemaining; initialPaymentRemaining = 0; remainingDebt
  }
}
