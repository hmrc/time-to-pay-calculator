package uk.gov.hmrc.selfservicetimetopay.models

import java.time.LocalDate

object InterestRate {
  object NONE extends InterestRate(LocalDate.MIN, None, 0)
}

case class InterestRate(startDate: LocalDate, endDate: Option[LocalDate], rate: BigDecimal) {
  def dailyRate = rate / startDate.lengthOfYear
}