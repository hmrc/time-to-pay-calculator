/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.timetopaycalculator.services

import java.time.LocalDate

import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.Logger
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.timetopaycalculator.models.{Calculation, Debit, Interest, PaymentSchedule}

import scala.io.Source

class CalculationServiceSpec extends UnitSpec with WithFakeApplication {

  val tolerance = 0.1

  case class MockInterestRateService(override val source: Source) extends InterestRateService

  class debit(amt: BigDecimal, calcTo: String, due: String) extends Debit(Some("POA1"), amt.setScale(2), Some(Interest(BigDecimal(0), LocalDate.parse(calcTo))), LocalDate.parse(due))

  "The calculator service" should {
    val table = Table(
      ("id", "debits",                                           "startDate",                    "endDate",                      "firstPaymentDate",            "initialPayment",           "repaymentCount", "amountToPay",  "totalInterest",  "regularAmount",  "finalAmount"),
/*
      NB: These tests reflect IDMS based data which uses a different calculation mechanism to IRIS. They are commented out as a result but left intact here for reference should we need to test against IDMS logic

      ("IDMS-A", Seq(new debit( 100.00, "2014-03-23", "2014-03-23"),
                new debit( 300.00, "2014-07-10", "2014-07-10")),  LocalDate.parse("2016-08-30"),  LocalDate.parse("2017-11-09"), LocalDate.parse("2016-08-30"),    30.00,                     15,               430.68,           30.68,           30.00,            30.00),
      ("IDMS-B", Seq(Debit("POA1", 0.0, Interest(406.89, LocalDate.now), LocalDate.now),
                new debit(1722.10, "2013-01-31", "2013-01-31"),
                new debit(  87.00, "2013-04-25", "2013-04-25"),
                new debit(  87.00, "2013-09-20", "2013-09-20"),
                new debit(  87.00, "2014-04-03", "2014-04-03"),
                new debit( 375.80, "2013-01-01", "2013-01-01"),
                new debit( 375.80, "2013-07-01", "2013-07-01"),
                new debit( 388.40, "2015-01-01", "2015-01-01"),
                new debit( 607.40, "2016-01-01", "2016-01-01")),  LocalDate.parse("2016-09-02"),  LocalDate.parse("2027-07-02"), LocalDate.parse("2016-08-30"),   385.00,                     130,              4870.60,        1140.10,           35.00,            35.00),
      ("IDMS-C", Seq(new debit(1784.53, "2016-07-31", "2016-07-31")),  LocalDate.parse("2016-09-09"),  LocalDate.parse("2017-09-29"),  LocalDate.parse("2016-08-30"),  149.18,                      13,              1811.45,          26.92,          149.18,           149.18),
*/
      ("IRIS-1", Seq(new debit(5000.0, "2016-09-03", "2016-09-03")), LocalDate.parse("2016-09-03"), LocalDate.parse("2017-03-10"), LocalDate.parse("2016-09-10"),   1500.0, 7, 5026.07, 26.07, 500.00, 526.07)
    )

    forAll(table) { (id, debits, startDate, endDate, firstPaymentDate, initialPayment, repaymentCount, amountToPay, totalInterest, regularAmount, finalAmount) =>
      s"calculate interest for scenario $id" in {
        val rates = "23 Aug 2016,2.75\n29 Sep 2009,3.00"
        val rateData = Source.fromChars(rates.toCharArray)

        def mockIRService = MockInterestRateService(source = rateData)
        def mockService = new CalculatorService(mockIRService, DurationService)

        val calculation = Calculation(debits, initialPayment, startDate, endDate, Some(firstPaymentDate), "MONTHLY")

        val schedule: PaymentSchedule = mockService.generateMultipleSchedules(calculation).head

        val amountPaid = schedule.instalments.map { _.amount }.sum

        val totalPaid = amountPaid + schedule.initialPayment

        Logger.info(s"Payment Schedule: Initial: ${schedule.initialPayment}, Over ${schedule.instalments.size}, Regular: ${schedule.instalments.head.amount}, Final: ${schedule.instalments.last.amount}, Total: $totalPaid")

        totalPaid.doubleValue() shouldBe (amountToPay.doubleValue() +- tolerance)
        schedule.totalInterestCharged.doubleValue() shouldBe (totalInterest.doubleValue() +- tolerance)

        val instalments = schedule.instalments

        instalments.size shouldBe repaymentCount
        instalments.head.amount shouldBe regularAmount
        instalments.last.amount.doubleValue() shouldBe (finalAmount.doubleValue() +- tolerance)
      }
    }

    val realWorldData = Table(
      ("debits", "startDate", "endDate", "initialPayment", "amountToPay", "instalmentBalance", "totalInterest", "totalPayable"),
      (Seq(new debit(0.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 0.0, 0.0, 0.0, 0.0),
      (Seq(new debit(1000.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 1000.0, 1000.0, 2.35, 1002.35),
      (Seq(new debit(500.0, "2016-09-01", "2016-09-01"),
        new debit(500.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 1000.0, 1000.0, 2.35, 1002.35),
      (Seq(new debit(1000.0, "2019-08-01", "2019-08-01")), LocalDate.parse("2019-08-01"), LocalDate.parse("2020-06-30"), 0.0, 1000.0, 1000.0, 11.58, 1011.58),
      (Seq(new debit(1000.0, "2016-01-01", "2016-01-01")), LocalDate.parse("2016-01-01"), LocalDate.parse("2016-11-30"), 0.0, 1000.0, 1000.0, 12.55, 1012.55),
      (Seq(new debit(1000.0, "2016-08-01", "2016-08-04")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 1000.0, 1000.0, 5.33, 1005.33),
      (Seq(new debit(1000.0, "2016-08-01", "2016-10-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 1000.0, 1000.0, 0.83, 1000.83),
      (Seq(new debit(1000.0, "2016-08-01", "2016-10-01"),
        new debit(500.0, "2016-09-01", "2016-09-01"),
        new debit(500.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 2000.0, 2000.0, 3.18, 2003.18),
      (Seq(new debit(1000.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 1000.0, 1000.0, 0.0, 0.00, 1000.00),
      (Seq(new debit(500.0, "2016-09-01", "2016-09-01"),
        new debit(500.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 1000.0, 1000.0, 0.0, 0.00, 1000.00),
      (Seq(new debit(500.0, "2016-09-01", "2016-09-01"),
        new debit(500.0, "2016-09-01", "2016-09-01"),
        new debit(500.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 1000.0, 1500.0, 500.0, 1.18, 1501.18)

    )
    
    forAll(realWorldData) { (debits, startDate, endDate, initialPayment, amountToPay, instalmentBalance, totalInterest, totalPayable) =>
      s"calculate interest of $totalInterest for an input debt of $debits to be paid between $startDate and $endDate with an initial payment of $initialPayment" in {
        val calculation = Calculation(debits, initialPayment, startDate, endDate, None, "MONTHLY")
        val schedule = new CalculatorService(InterestRateService, DurationService).generateMultipleSchedules(calculation).head
        schedule.amountToPay.doubleValue() shouldBe (amountToPay.doubleValue() +- tolerance)
        schedule.initialPayment shouldBe initialPayment
        schedule.instalmentBalance shouldBe instalmentBalance
        schedule.totalInterestCharged.doubleValue() shouldBe (totalInterest.doubleValue() +- tolerance)
        schedule.totalPayable shouldBe totalPayable
      }
    }

    val instalmentCalcData = Table(
      ("debits", "startDate", "endDate", "initialPayment", "months"),
      (Seq(new debit(0.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 3),
      (Seq(new debit(1000.0, "2016-09-01", "2016-09-01")), LocalDate.parse("2016-09-01"), LocalDate.parse("2016-11-30"), 0.0, 3)
    )

    forAll(instalmentCalcData) { (debits, startDate, endDate, initialPayment, months) =>
      s"generate an instalment plan of $months for a given debt $debits and period from $startDate to $endDate" in {
        val calculation = Calculation(debits, initialPayment, startDate, endDate, None, "MONTHLY")
        val schedule = new CalculatorService(InterestRateService, DurationService).generateMultipleSchedules(calculation).head

        schedule.instalments.size shouldBe months
        schedule.instalments.map {
          _.amount
        }.fold(BigDecimal(0)) { (a, it) => a + it }.doubleValue() shouldBe ((schedule.totalInterestCharged + schedule.instalmentBalance).doubleValue() +- tolerance)
      }
    }
  }
}
