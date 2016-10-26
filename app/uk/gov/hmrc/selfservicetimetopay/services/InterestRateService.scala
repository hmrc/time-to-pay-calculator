package uk.gov.hmrc.selfservicetimetopay.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import uk.gov.hmrc.selfservicetimetopay.models.InterestRate

import scala.io.Source

class InterestRateService extends InterestRateService {
  val filename = "/interestRates.csv"
}

trait InterestRateService {
  val filename: String = ???
  val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE:dd MMM yyyy")

  lazy val rates: Seq[InterestRate] = streamInterestRates(filename)

  private val interestRateConsumer = { line: String =>
    val (date, rate) = line.split(",")
    val endDate: Option[LocalDate] = rates.lastOption.map(ir => ir.startDate.minusDays(1))
    InterestRate(date, endDate, rate)
  }

  private def streamInterestRates(fileName: String): Seq[InterestRate] = {
    Source.fromFile(fileName).getLines().map(interestRateConsumer).toSeq
  }

  def getAllRates: Seq[InterestRate] = {
    rates
  }

  def getRateAt(date: LocalDate): Option[InterestRate] = {
    rates.find(rate => rate.startDate.compareTo(date) <= 0)
  }

  def getRatesForPeriod(startDate: LocalDate, endDate: LocalDate): Seq[InterestRate] = {
    rates.filter(interestRate -> (interestRate.getStartDate().compareTo(endDate) <= 0) &&
      (interestRate.getEndDate() == null || interestRate.getEndDate().compareTo(startDate) >= 0)).flatMap(interestRate -> {
      int startYear = startDate.isAfter(interestRate.getStartDate()) ? startDate.getYear(): interestRate.getStartDate ().getYear();
      int endYear = interestRate.getEndDate() == null ? endDate.getYear(): interestRate.getEndDate ().getYear();

      return IntStream.rangeClosed(startYear, endYear).mapToObj(year -> new InterestRate(year == startYear ? interestRate.getStartDate(): LocalDate.of(year, 1, 1),
        year == endYear ? interestRate.getEndDate(): LocalDate.of(year, 12, 31), interestRate.getRate()) );
    }).sorted(InterestRate.compareTo).collect(Collectors.toList)
  }
}
