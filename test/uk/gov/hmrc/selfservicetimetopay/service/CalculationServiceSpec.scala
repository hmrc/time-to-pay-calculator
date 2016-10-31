package uk.gov.hmrc.selfservicetimetopay.service

import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

/**
  * Created by andrew on 31/10/16.
  */
class CalculationServiceSpec extends UnitSpec with WithFakeApplication {
/*
  @Shared
  private service = SsttpServicesConfig.calculatorService

  //    @Ignore("Selecta scenario results are questionable")
  @Unroll
  def "Selecta based testing over #repaymentCount months at #rate% Scenario \\##scenrioId"() {
    given:
    def mockIRService = new InterestRateService()
    mockIRService.rates = [new InterestRate(rateFrom, null, rate)]
    def mockService = new CalculatorService(SsttpServicesConfig.validatorService, mockIRService, SsttpServicesConfig.durationService)

    when:
    def calculation = new Calculation(liabilities, initialPayment, startDate, endDate, "MONTHLY")

    then:
    def schedule = mockService.generateMultipleSchedules(calculation)[0]
    schedule.getAmountToPay() == amountToPay
    schedule.getTotalInterestCharged() == totalInterest

    def instalments = schedule.getInstalments()

    instalments.size() == repaymentCount
    instalments[0].amount == regularAmount
    instalments[repaymentCount -1].amount == finalAmount

    where:
      liabilities                                             |   rate        |   rateFrom                        |   startDate                       |   endDate                                         |   initialPayment ||   repaymentCount  |   amountToPay |   totalInterest      |   regularAmount   |   finalAmount
        [liability(100.0, "2014-03-23", "2014-03-23"),
    liability(300.0, "2014-07-10", "2014-07-10")]          |   2.75        |   LocalDate.parse("2016-08-23")   |   LocalDate.parse("2016-08-30")   |   LocalDate.parse("2017-11-09")                   |     30.0         ||      15           |     400.0     |      30.02           |   30.00           |       10.68
      [liability(5000.0, "2016-09-03", "2016-09-03")]         |   2.75        |   LocalDate.parse("2016-08-23")   |   LocalDate.parse("2016-11-01")   |   LocalDate.parse("2017-09-01")                   |       0.0        ||      11           |    5000.0     |      78.21           |   461.65          |      461.71
  }

  @Unroll
  def "For an input debt amount of #liabilities to be paid between #startDate and #endDate with an initial payment of #initialPayment the total interest should be #totalInterest"() {
    when:
    def calculation = new Calculation(liabilities, initialPayment, startDate, endDate, "MONTHLY")

    then:
    def schedule = service.generateMultipleSchedules(calculation)[0]
    schedule.getAmountToPay() == amountToPay
    schedule.getInitialPayment() == initialPayment
    schedule.getInstalmentBalance() == instalmentBalance
    schedule.getTotalInterestCharged() == totalInterest
    schedule.getTotalPayable() == totalPayable

    where:
      liabilities                                             |   startDate                       |   endDate                         |   initialPayment ||   amountToPay |   instalmentBalance   |   totalInterest   |   totalPayable
        [liability(0.0, "2016-09-01", "2016-09-01")]            |   LocalDate.parse("2016-09-01")   |   LocalDate.parse("2016-11-30")   |     0.0          ||      0.0      |      0.0              |   0.0             |      0.0
      [liability(1000.0, "2016-09-01", "2016-09-01")]         |   LocalDate.parse("2016-09-01")   |   LocalDate.parse("2016-11-30")   |     0.0          ||   1000.0      |   1000.0              |   0.42            |   1000.42
      [liability(500.0, "2016-09-01", "2016-09-01"),
    liability(500.0, "2016-09-01", "2016-09-01")]          |   LocalDate.parse("2016-09-01")   |   LocalDate.parse("2016-11-30")   |     0.0          ||   1000.0      |   1000.0              |   0.42            |   1000.42
      [liability(1000.0, "2019-08-01", "2019-08-01")]         |   LocalDate.parse("2019-08-01")   |   LocalDate.parse("2020-06-30")   |     0.0          ||   1000.0      |   1000.0              |   1.25            |   1001.25
      [liability(1000.0, "2016-01-01", "2016-01-01")]         |   LocalDate.parse("2016-01-01")   |   LocalDate.parse("2016-11-30")   |     0.0          ||   1000.0      |   1000.0              |   2.05            |   1002.05
      [liability(1000.0, "2016-08-01", "2016-08-04")]         |   LocalDate.parse("2016-09-01")   |   LocalDate.parse("2016-11-30")   |     0.0          ||   1000.0      |   1000.0              |   0.61            |   1000.61
      [liability(1000.0, "2016-08-01", "2016-10-01")]         |   LocalDate.parse("2016-09-01")   |   LocalDate.parse("2016-11-30")   |     0.0          ||   1000.0      |   1000.0              |   0.31            |   1000.31
      [liability(1000.0, "2016-08-01", "2016-10-01"),
    liability(500.0, "2016-09-01", "2016-09-01"),
    liability(500.0, "2016-09-01", "2016-09-01")]          |   LocalDate.parse("2016-09-01")   |   LocalDate.parse("2016-11-30")   |     0.0          ||   2000.0      |   2000.0              |   0.73            |   2000.73
      [liability(1000.0, "2016-09-01", "2016-09-01")]         |   LocalDate.parse("2016-09-01")   |   LocalDate.parse("2016-11-30")   |  1000.0          ||   1000.0      |      0.0              |   0.00            |   1000.00
      [liability(500.0, "2016-09-01", "2016-09-01"),
    liability(500.0, "2016-09-01", "2016-09-01")]          |   LocalDate.parse("2016-09-01")   |   LocalDate.parse("2016-11-30")   |  1000.0          ||   1000.0      |      0.0              |   0.00            |   1000.00
      [liability(500.0, "2016-09-01", "2016-09-01"),
    liability(500.0, "2016-09-01", "2016-09-01"),
    liability(500.0, "2016-09-01", "2016-09-01")]          |   LocalDate.parse("2016-09-01")   |   LocalDate.parse("2016-11-30")   |  1000.0          ||   1500.0      |    500.0              |   0.21            |   1500.21
  }

  @Unroll
  def "For a given debt for period #startDate to #endDate we can generate an instalment plan of #months"() {
    when:
    def calculation = new Calculation(liabilities, initialPayment, startDate, endDate, "MONTHLY")

    then:
    def schedule = service.generateMultipleSchedules(calculation)[0]
    schedule.getInstalments().size() == months
    schedule.instalments.sum { it.amount } == schedule.totalInterestCharged + schedule.instalmentBalance

    where:
      liabilities                                             |   startDate                       |   endDate                         |   initialPayment ||   months
        [liability(0.0, "2016-09-01", "2016-09-01")]            |   LocalDate.parse("2016-09-01")   |   LocalDate.parse("2016-11-30")   |     0.0          ||   3
      [liability(1000.0, "2016-09-01", "2016-09-01")]         |   LocalDate.parse("2016-09-01")   |   LocalDate.parse("2016-11-30")   |     0.0          ||   3

  }

  private static def liability(BigDecimal amt, String intCalcTo, String dueDate) {
    new Liability("POA1", amt, 0, LocalDate.parse(intCalcTo), LocalDate.parse(dueDate))
  }
*/

}
