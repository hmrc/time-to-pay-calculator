/*
 * Copyright 2021 HM Revenue & Customs
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

import support.ITSpec

import java.time.LocalDate
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.Logger
import timetopaycalculator.cor.model.{CalculatorInput, DebitInput, PaymentSchedule}

class CalculationServiceSpe

class CalculationServiceSpec extends ITSpec {

  private val calculatorService = fakeApplication().injector.instanceOf[CalculatorService]

  private val logger = Logger(getClass)

  def debit(amt: BigDecimal, due: String) = DebitInput(amount  = amt.setScale(2), dueDate = LocalDate.parse(due))
  def date(date: String): LocalDate = LocalDate.parse(date)

  val interestCalculationScenarios = Table(
    ("id", "debits", "startDate", "endDate", "firstPaymentDate", "initialPayment", "duration", "totalPayable", "totalInterestCharged", "regularInstalmentAmount", "finalInstalmentAmount"),
    ("1.a.i.c", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2018-01-20"), date("2017-04-20"), 0, 10, 2032.74, 32.74, 200.00, 232.74),
    ("1.a.ii.c", Seq(debit(2000.00, "2015-01-31")), date("2016-03-14"), date("2017-01-20"), date("2016-04-20"), 0, 10, 2095.61, 95.61, 200.00, 295.61),
    ("1.b.ii.c", Seq(debit(2000.00, "2016-01-31")), date("2017-03-14"), date("2018-01-20"), date("2017-04-20"), 0, 10, 2090.39, 90.39, 200.00, 290.39),
    ("1.d", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2018-01-20"), date("2017-04-20"), 1000, 10, 2019.54, 19.54, 100.00, 119.54),
    ("1.e", Seq(debit(2000.00, "2017-01-31"), debit(1000.00, "2017-02-01")), date("2017-03-14"), date("2018-01-20"), date("2017-04-20"), 2000, 10, 3023.68, 23.68, 100.00, 123.68),
    ("1.f", Seq(debit(2000.00, "2017-01-31"), debit(2000.00, "2017-02-01")), date("2017-03-14"), date("2018-01-20"), date("2017-04-20"), 2500, 10, 4032.85, 32.85, 150.00, 182.85),
    ("2.a", Seq(debit(2000.00, "2017-03-31")), date("2017-03-14"), date("2018-01-20"), date("2017-04-20"), 0, 10, 2023.85, 23.85, 200.00, 223.85),
    ("2.b", Seq(debit(2000.00, "2017-03-18")), date("2017-03-14"), date("2018-01-20"), date("2017-04-20"), 1000, 10, 2012.91, 12.91, 100.00, 112.91),
    ("2.c", Seq(debit(2000.00, "2017-03-18"), debit(2000.00, "2017-03-19")), date("2017-03-14"), date("2018-01-20"), date("2017-04-20"), 2000, 10, 4026.11, 26.11, 200.00, 226.11),
    ("2.d", Seq(debit(2000.00, "2017-03-18"), debit(2000.00, "2017-03-19")), date("2017-03-14"), date("2018-01-20"), date("2017-04-20"), 2500, 10, 4019.55, 19.55, 150.00, 169.55),
    ("2.e", Seq(debit(2000.00, "2017-03-31")), date("2017-03-14"), date("2018-01-20"), date("2017-04-20"), 1000, 10, 2011.93, 11.93, 100.00, 111.93)
  )

  forAll(interestCalculationScenarios) { (id, debits, startDate, endDate, firstPaymentDate, initialPayment, duration, totalPayable, totalInterestCharged, regularInstalmentAmount, finalInstalmentAmount) =>
    s"The calculator service should, for $id calculate totalInterestCharged of $totalInterestCharged with totalPayable of $totalPayable, regularInstalmentAmount of $regularInstalmentAmount and finalInstalmentAmount of $finalInstalmentAmount" in {

      val calculation = CalculatorInput(debits, initialPayment, startDate, endDate, Some(firstPaymentDate))

      val schedule: PaymentSchedule = calculatorService.buildSchedule(calculation)

      val amountPaid = schedule.instalments.map { _.amount }.sum

      val totalPaid = amountPaid + schedule.initialPayment

      logger.info(s"Payment Schedule: Initial: ${schedule.initialPayment}, Over ${schedule.instalments.size}, Regular: ${schedule.instalments.head.amount}, Final: ${schedule.instalments.last.amount}, Total: $totalPaid")

      totalPaid.doubleValue() mustBe totalPayable.doubleValue()
      schedule.totalInterestCharged.doubleValue() mustBe totalInterestCharged.doubleValue()

      val instalments = schedule.instalments

      instalments.size mustBe duration
      instalments.head.amount mustBe regularInstalmentAmount
      instalments.last.amount.doubleValue() mustBe finalInstalmentAmount.doubleValue()
    }
  }

  val regularPaymentDateScenarios = Table(
    ("id", "debits", "startDate", "endDate", "firstPaymentDate", "initialPayment", "duration"),
    ("1.i", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2017-12-21"), date("2017-03-21"), 0, 10),
    ("1.ii", Seq(debit(2000.00, "2015-01-31")), date("2015-03-14"), date("2015-12-21"), date("2015-03-21"), 0, 10),
    ("1.iii", Seq(debit(2000.00, "2016-01-31")), date("2016-03-14"), date("2016-12-21"), date("2016-03-21"), 0, 10),
    ("2.i", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2018-01-02"), date("2017-04-02"), 0, 10),
    ("2.ii", Seq(debit(2000.00, "2015-01-31")), date("2016-03-14"), date("2017-01-02"), date("2016-04-02"), 0, 10),
    ("2.iii", Seq(debit(2000.00, "2016-01-31")), date("2017-03-14"), date("2018-01-02"), date("2017-04-02"), 0, 10),
    ("3.i", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2017-12-21"), date("2017-03-21"), 1000, 10),
    ("3.ii", Seq(debit(2000.00, "2017-01-31"), debit(1000.00, "2017-02-01")), date("2017-03-14"), date("2018-01-21"), date("2017-04-21"), 2000, 10),
    ("3.iii", Seq(debit(2000.00, "2017-01-31"), debit(2000.00, "2017-02-01")), date("2017-03-14"), date("2018-01-21"), date("2017-04-21"), 2500, 10),
    ("4.i", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2018-01-02"), date("2017-04-02"), 1000, 10),
    ("4.ii", Seq(debit(2000.00, "2017-01-31"), debit(1000.00, "2017-02-01")), date("2017-03-14"), date("2018-01-02"), date("2017-04-02"), 2000, 10),
    ("4.iii", Seq(debit(2000.00, "2017-01-31"), debit(2000.00, "2017-02-01")), date("2017-03-14"), date("2018-01-02"), date("2017-04-02"), 2500, 10),
    ("5.i", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2018-01-14"), date("2017-04-14"), 1000, 10),
    ("6.i", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2018-01-02"), date("2017-04-02"), 1000, 10),
    ("7.i", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2018-01-21"), date("2017-04-21"), 1000, 10)
  )

  forAll(regularPaymentDateScenarios) { (id, debits, startDate, endDate, firstPaymentDate, initialPayment, duration) =>
    s"The calculator service should, for $id calculate a duration of $duration" in {

      val calculation = CalculatorInput(debits, initialPayment, startDate, endDate, Some(firstPaymentDate))

      val schedule: PaymentSchedule = calculatorService.buildSchedule(calculation)

      val amountPaid = schedule.instalments.map { _.amount }.sum

      val totalPaid = amountPaid + schedule.initialPayment

      logger.info(s"Payment Schedule: Initial: ${schedule.initialPayment}, Over ${schedule.instalments.size}, Regular: ${schedule.instalments.head.amount}, Final: ${schedule.instalments.last.amount}, Total: $totalPaid")

      schedule.instalments.size mustBe duration
    }
  }
}
