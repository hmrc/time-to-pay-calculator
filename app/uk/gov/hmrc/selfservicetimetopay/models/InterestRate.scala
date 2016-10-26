package uk.gov.hmrc.selfservicetimetopay.models

import java.time.LocalDate

case class InterestRate(startDate: LocalDate, endDate: Option[LocalDate], rate: BigDecimal)