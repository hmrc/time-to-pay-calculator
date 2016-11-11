/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.selfservicetimetopay.services

import java.time.LocalDate

import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.Logger
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.models.{Calculation, Liability, PaymentSchedule}

import scala.io.Source

class CalculationServiceSpec extends UnitSpec with WithFakeApplication {

  case class MockInterestRateService(override val source: Source) extends InterestRateService

  class liability(amt: BigDecimal, calcTo: String, due: String) extends Liability("POA1", amt.setScale(2), BigDecimal(0), LocalDate.parse(calcTo), LocalDate.parse(due))

  "The calculator service" should {
    val table = Table(
      ("id", "liabilities",                                           "startDate",                    "endDate",                      "initialPayment",           "repaymentCount", "amountToPay",  "totalInterest",  "regularAmount",  "finalAmount"),
      ("A", Seq(new liability( 100.00, "2014-03-23", "2014-03-23"),
                new liability( 300.00, "2014-07-10", "2014-07-10")),  LocalDate.parse("2016-08-30"),  LocalDate.parse("2017-11-09"),   30.00,                     15,               430.68,           30.68,           30.00,            30.00),
      ("B", Seq(Liability("POA1", 0.0, 406.89, LocalDate.now, LocalDate.now),
                new liability(1722.10, "2013-01-31", "2013-01-31"),
                new liability(  87.00, "2013-04-25", "2013-04-25"),
                new liability(  87.00, "2013-09-20", "2013-09-20"),
                new liability(  87.00, "2014-04-03", "2014-04-03"),
                new liability( 375.80, "2013-01-01", "2013-01-01"),
                new liability( 375.80, "2013-07-01", "2013-07-01"),
                new liability( 388.40, "2015-01-01", "2015-01-01"),
                new liability( 607.40, "2016-01-01", "2016-01-01")),  LocalDate.parse("2016-09-02"),  LocalDate.parse("2027-07-02"),  385.00,                     130,              4870.60,        1140.10,           35.00,            35.00),
      ("C", Seq(new liability(1784.53, "2016-07-31", "2016-07-31")),  LocalDate.parse("2016-09-09"),  LocalDate.parse("2017-09-29"),  149.18,                     13,               1811.45,          26.92,          149.18,           149.18)
    )

    forAll(table) { (id, liabilities, startDate, endDate, initialPayment, repaymentCount, amountToPay, totalInterest, regularAmount, finalAmount) =>
      s"calculate interest for IDMS scenario $id" in {
        val rates = "Tue:23 Aug 2016,2.75\nTue:29 Sep 2009,3.00"
        val rateData = Source.fromChars(rates.toCharArray)

        def mockIRService = MockInterestRateService(source = rateData)
        def mockService = new CalculatorService(mockIRService, DurationService)

        val calculation = Calculation(liabilities, initialPayment, startDate, endDate, "MONTHLY")

        val schedule: PaymentSchedule = mockService.generateMultipleSchedules(calculation).head

        val amountPaid = schedule.instalments.map { _.amount }.sum

        val totalPaid = amountPaid + schedule.initialPayment

        Logger.info(s"Payment Schedule: Initial: ${schedule.initialPayment}, Over ${schedule.instalments.size}, Regular: ${schedule.instalments.head.amount}, Final: ${schedule.instalments.last.amount}, Total: $totalPaid")

        totalPaid shouldBe amountToPay
        schedule.totalInterestCharged shouldBe totalInterest

        val instalments = schedule.instalments

        instalments.size shouldBe repaymentCount
        instalments.head shouldBe regularAmount
        instalments.last.amount shouldBe finalAmount
      }
    }

    val realWorldData = Table(
      ("liabilities", "startDate", "endDate", "initialPayment", "amountToPay", "instalmentBalance", "totalInterest", "totalPayable"),
      (Seq(new liability(0.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 0.0, 0.0, 0.0, 0.0),
      (Seq(new liability(1000.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 1000.0, 1000.0, 0.42, 1000.42),
      (Seq(new liability(500.0, "2016-09-01", "2016-09-01"),
        new liability(500.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 1000.0, 1000.0, 0.42, 1000.42),
      (Seq(new liability(1000.0, "2019-08-01", "2019-08-01")), LocalDate.parse("2019-08-01"), LocalDate.parse("2020-06-30"), 0.0, 1000.0, 1000.0, 1.20, 1001.20),
      (Seq(new liability(1000.0, "2016-01-01", "2016-01-01")), LocalDate.parse("2016-01-01"), LocalDate.parse("2016-11-30"), 0.0, 1000.0, 1000.0, 2.50, 1002.50),
      (Seq(new liability(1000.0, "2016-08-01", "2016-08-04")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 1000.0, 1000.0, 0.61, 1000.61),
      (Seq(new liability(1000.0, "2016-08-01", "2016-10-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 1000.0, 1000.0, 0.31, 1000.31),
      (Seq(new liability(1000.0, "2016-08-01", "2016-10-01"),
        new liability(500.0, "2016-09-01", "2016-09-01"),
        new liability(500.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 2000.0, 2000.0, 0.73, 2000.73),
      (Seq(new liability(1000.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 1000.0, 1000.0, 0.0, 0.00, 1000.00),
      (Seq(new liability(500.0, "2016-09-01", "2016-09-01"),
        new liability(500.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 1000.0, 1000.0, 0.0, 0.00, 1000.00),
      (Seq(new liability(500.0, "2016-09-01", "2016-09-01"),
        new liability(500.0, "2016-09-01", "2016-09-01"),
        new liability(500.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 1000.0, 1500.0, 500.0, 0.21, 1500.21)
    )
    
    forAll(realWorldData) { (liabilities, startDate, endDate, initialPayment, amountToPay, instalmentBalance, totalInterest, totalPayable) =>
      s"calculate interest of $totalInterest for an input debt of $liabilities to be paid between $startDate and $endDate with an initial patment of $initialPayment" in {
        val calculation = Calculation(liabilities, initialPayment, startDate, endDate, "MONTHLY")
        val schedule = new CalculatorService(InterestRateService, DurationService).generateMultipleSchedules(calculation).head
        schedule.amountToPay shouldBe amountToPay
        schedule.initialPayment shouldBe initialPayment
        schedule.instalmentBalance shouldBe instalmentBalance
        schedule.totalInterestCharged shouldBe totalInterest
        schedule.totalPayable shouldBe totalPayable
      }
    }

    val instalmentCalcData = Table(
      ("liabilities", "startDate", "endDate", "initialPayment", "months"),
      (Seq(new liability(0.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 3),
      (Seq(new liability(1000.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 3)
    )

    forAll(instalmentCalcData) { (liabilities, startDate, endDate, initialPayment, months) =>
      s"generate an instalment plan of $months for a given debt $liabilities and period from $startDate to $endDate" in {
        val calculation = Calculation(liabilities, initialPayment, startDate, endDate, "MONTHLY")
        val schedule = new CalculatorService(InterestRateService, DurationService).generateMultipleSchedules(calculation).head

        schedule.instalments.size shouldBe months
        schedule.instalments.map { _.amount }.fold(BigDecimal(0)) { (a, it) => a + it } shouldBe schedule.totalInterestCharged + schedule.instalmentBalance
      }
    }
  }
}
