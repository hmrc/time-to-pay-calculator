package uk.gov.hmrc.selfservicetimetopay.models

import java.time.LocalDate

case class Calculation(liabilities: Seq[Liability], initialPayment: BigDecimal, startDate: LocalDate, endDate: LocalDate, paymentFrequency: String)
