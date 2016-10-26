package uk.gov.hmrc.selfservicetimetopay.services

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit._

object DurationService {
  def getDaysBetween(startDate: LocalDate, endDate: LocalDate): Long = calculatePeriod(startDate, endDate, DAYS, inclusive = false)

  def getRepaymentDates(startDate: LocalDate, endDate: LocalDate): Seq[LocalDate] = {
    val firstPaymentDate = startDate match {
      case d if d.getDayOfMonth == 1 => startDate
      case _ => startDate.plusMonths(1).withDayOfMonth(1)
    }

    Iterator.iterate(firstPaymentDate)(_ plusMonths 1).takeWhile(_ isBefore endDate).toSeq
  }

  private def calculatePeriod(startDate: LocalDate, endDate: LocalDate, frequency: ChronoUnit, inclusive: Boolean): Long = {
    frequency.between(startDate, endDate) + (if (inclusive) 1 else 0) match {
      case c if c > 0 => c
      case _ => 0
    }
  }
}
