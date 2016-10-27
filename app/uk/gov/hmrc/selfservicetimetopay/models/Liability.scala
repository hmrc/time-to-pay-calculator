package uk.gov.hmrc.selfservicetimetopay.models

import java.time.LocalDate

case class Liability(`type`: String, amount: BigDecimal, interestAccrued: BigDecimal, interestCalculationDate: LocalDate, dueDate: LocalDate, endDate: LocalDate, rate: InterestRate)

object Liability {
  def partialOf(l: Liability, startDate: LocalDate, endDate: LocalDate, rate: InterestRate): Liability = {
    Liability(l.`type`, l.amount, l.interestAccrued, l.interestCalculationDate, startDate, endDate, rate)
  }
}