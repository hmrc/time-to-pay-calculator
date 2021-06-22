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

package uk.gov.hmrc.timetopaycalculator.controllers

import java.time.LocalDate

import support.ITSpec
import timetopaycalculator.cor.model.{CalculatorInput, DebitInput, Instalment, PaymentSchedule}
import timetopaytaxpayer.cor.CalculatorConnector
import uk.gov.hmrc.http.HeaderCarrier

object TdAll {

  val calculatorInput = CalculatorInput(
    debits           = List(
      DebitInput(
        500,
        "2019-06-07"
      ),
      DebitInput(
        700,
        "2019-09-13"
      )
    ),
    initialPayment   = 123.11,
    startDate        = "2019-01-31",
    endDate          = "2020-02-18",
    firstPaymentDate = "2019-02-18"
  )

  implicit def toSome[T](t: T): Option[T] = Some(t)

  implicit def toLocalDate(t: String): LocalDate = LocalDate.parse(t)

  implicit def toSomeLocalDate(t: String): Option[LocalDate] = Some(LocalDate.parse(t))

  implicit def toBigDecimal(s: String): BigDecimal = BigDecimal(s)
}

class PaymentCalculationControllerSpec extends ITSpec {

  import TdAll._

  "compute calculation" in {

    val connector = app.injector.instanceOf[CalculatorConnector]

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val result = connector.calculatePaymentschedule(TdAll.calculatorInput).futureValue

    result shouldBe PaymentSchedule(
      startDate            = "2019-01-31",
      endDate              = "2020-02-18",
      initialPayment       = "123.11",
      amountToPay          = 1200,
      instalmentBalance    = "1076.89",
      totalInterestCharged = "5.48",
      totalPayable         = "1205.48",
      instalments          = List(
        Instalment("2019-02-18", "82.84", "0"),
        Instalment("2019-03-18", "82.84", "0"),
        Instalment("2019-04-18", "82.84", "0"),
        Instalment("2019-05-18", "82.84", "0"),
        Instalment("2019-06-18", "82.84", "0.03097561643835616438356164383561644"),
        Instalment("2019-07-18", "82.84", "0.1084146575342465753424657534246575"),
        Instalment("2019-08-18", "82.84", "0.188435"),
        Instalment("2019-09-18", "82.84", "0.2972245205479452054794520547945206"),
        Instalment("2019-10-18", "82.84", "0.5185094520547945205479452054794521"),
        Instalment("2019-11-18", "82.84", "0.7471705479452054794520547945205479"),
        Instalment("2019-12-18", "82.84", "0.9684554794520547945205479452054796"),
        Instalment("2020-01-18", "82.84", "1.197116575342465753424657534246575"),
        Instalment("2020-02-18", "88.32", "1.425777671232876712328767123287671")
      )
    )
  }

}
