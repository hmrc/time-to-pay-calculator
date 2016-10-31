package uk.gov.hmrc.selfservicetimetopay.service

import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class DurationServiceSpec extends UnitSpec with WithFakeApplication {
/*
  @Shared
  private DurationService service = new DurationService();

  @Unroll
  def "Given a start date of #startDate and end date #endDate repayment periods should be #periods (#description)"() {
    expect:
      service.getRepaymentDates(startDate, endDate).size() == periods

    where:
      startDate                       |   endDate                         |   periods | description
    LocalDate.parse("2016-01-01")   |   LocalDate.parse("2016-02-28")   |    2      | "2 periods for 2 months exactly"
    LocalDate.parse("2016-01-01")   |   LocalDate.parse("2016-03-01")   |    3      | "3 periods for 2 months + 1 day"
    LocalDate.parse("2015-12-30")   |   LocalDate.parse("2016-03-01")   |    3      | "3 periods for 2 months + 2 days"
    LocalDate.parse("2016-01-01")   |   LocalDate.parse("2016-03-15")   |    3      | "3 periods for 2 months + 14 days"
    LocalDate.parse("2016-01-01")   |   LocalDate.parse("2016-03-31")   |    3      | "3 periods for 2 months + 30 days"
    LocalDate.parse("2016-01-01")   |   LocalDate.parse("2016-03-31")   |    3      | "4 periods for 3 months exactly"
    LocalDate.parse("2016-01-01")   |   LocalDate.parse("2016-04-01")   |    4      | "4 periods for 3 months + 1 day"
  }

  @Unroll
  def "Given invalid input of #startDate and #endDate an exception is thrown"() {
    when:
      service.getRepaymentDates(startDate, endDate)

    then:
      thrown IllegalArgumentException

        where:
    startDate                       |   endDate                         |   periods | description
    //        Test scenarios removed - no longer alidate a min/max length of periods
    //        LocalDate.parse("2016-01-01")   |   LocalDate.parse("2016-01-01")   |    0      | "0 periods for same date"
    //        LocalDate.parse("2016-01-01")   |   LocalDate.parse("2016-01-31")   |    1      | "0 periods for <= 1 month"
    //        LocalDate.parse("2016-01-01")   |   LocalDate.parse("2017-01-01")   |   12      | "11 periods for 1 year"
    LocalDate.parse("2017-01-01")   |   LocalDate.parse("2016-04-01")   |    0      | "Should return 0 if startDate is after endDate"
  }

  @Unroll
  def "Number of days between #startDate and #endDate should be #count"() {
    expect:
      service.getDaysBetween(startDate, endDate) == count

    where:
      startDate                       |   endDate                         |   count
    LocalDate.parse("2016-01-01")   |   LocalDate.parse("2016-01-01")   |   1
    LocalDate.parse("2016-01-01")   |   LocalDate.parse("2016-01-02")   |   2
    LocalDate.parse("2016-01-01")   |   LocalDate.parse("2016-01-03")   |   3
    LocalDate.parse("2015-01-01")   |   LocalDate.parse("2016-12-31")   |   365 + 366
    LocalDate.parse("2017-01-01")   |   LocalDate.parse("2016-01-03")   |   0
  }
*/

}
