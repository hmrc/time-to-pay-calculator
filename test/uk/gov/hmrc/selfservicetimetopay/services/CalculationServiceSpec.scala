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
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.models.{Calculation, Liability, PaymentSchedule}

import scala.io.Source

class CalculationServiceSpec extends UnitSpec with WithFakeApplication {

  case class MockCalculatorService(override val interestService: InterestRateService, override val durationService: DurationService) extends CalculatorService

  case class MockInterestRateService(override val source: Source) extends InterestRateService

  class liability(amt: BigDecimal, calcTo: String, due: String) extends Liability("POA1", amt, BigDecimal(0), LocalDate.parse(calcTo), LocalDate.parse(due))

  "The calculator service" should {
    val table = Table(
      ("id", "liabilities", "rate", "startDate", "endDate", "initialPayment", "repaymentCount", "amountToPay", "totalInterest", "regularAmount", "finalAmount"),
      (1, Seq(new liability(100.0, "2014-03-23", "2014-03-23"),
        new liability(300.0, "2014-07-10", "2014-07-10")), "Tue:23 Aug 2016,2.75", LocalDate.parse("2016-08-30"), LocalDate.parse("2017-11-09"), 30.0, 15, 400.0, 30.02, 30.00, 10.68),
      (2, Seq(new liability(5000.0, "2016-09-03", "2016-09-03")), "Tue:23 Aug 2016,2.75", LocalDate.parse("2016-11-01"), LocalDate.parse("2017-09-01"), 0.0, 11, 5000.0, 78.21, 461.65, 461.71)
    )

    forAll(table) { (id, liabilities, rate, startDate, endDate, initialPayment, repaymentCount, amountToPay, totalInterest, regularAmount, finalAmount) =>
      s"calculate interest for $repaymentCount months at $rate% for scenario #$id" ignore {
        val rateData = Source.fromChars(rate.toCharArray)

        def mockIRService = MockInterestRateService(source = rateData)
        def mockService = MockCalculatorService(mockIRService, DurationService)

        val calculation = Calculation(liabilities, initialPayment, startDate, endDate, "MONTHLY")

        val schedule: PaymentSchedule = mockService.generateMultipleSchedules(calculation).head
        schedule.amountToPay shouldBe amountToPay
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
        val schedule = CalculatorService.generateMultipleSchedules(calculation).head
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
        val schedule = CalculatorService.generateMultipleSchedules(calculation).head

        schedule.instalments.size shouldBe months
        schedule.instalments.map { _.amount }.fold(BigDecimal(0)) { (a, it) => a + it } shouldBe schedule.totalInterestCharged + schedule.instalmentBalance
      }
    }
  }
}
