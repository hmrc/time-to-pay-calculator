package uk.gov.hmrc.selfservicetimetopay.models

import java.time.LocalDate

case class Liability(`type`: String, amount: BigDecimal, interestAccrued: BigDecimal, interestCalculationDate: LocalDate, dueDate: LocalDate, endDate: LocalDate, rate: InterestRate)
