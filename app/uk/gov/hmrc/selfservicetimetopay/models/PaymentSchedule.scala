package uk.gov.hmrc.selfservicetimetopay.models

import java.time.LocalDate

case class PaymentSchedule(initialPayment: BigDecimal, amountToPay: BigDecimal, instalmentBalance: BigDecimal, totalInterestCharged: BigDecimal, totalPayable: BigDecimal, instalments: Seq[Instalment])

case class Instalment(paymentDate: LocalDate, amount: BigDecimal)