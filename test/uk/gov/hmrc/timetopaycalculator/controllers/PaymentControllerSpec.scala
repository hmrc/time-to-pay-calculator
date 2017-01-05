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

package uk.gov.hmrc.timetopaycalculator.controllers

import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.mvc.Http.Status._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.timetopaycalculator.models.PaymentSchedule

class PaymentControllerSpec extends UnitSpec with WithFakeApplication {

  "The payment controller" should {
    val inputStatusData = Table(
      ("input", "statusCode"),
      ("{}", BAD_REQUEST),
      ("""{ "initialPayment": 250.0, "startDate": "26-10-2015" }""", BAD_REQUEST),
      (
        """{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY",
             "debits": [{"originCode": "PAO1", "amount": 1000.00,
              "interest": {"amountAccrued" : 0.0, "calculationDate" : "2016-09-01"},
              "dueDate" : "2016-09-01"}]}""", OK),
      (
        """{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY",
             "debits": [{"amount": 1000.00,
              "interest": {"amountAccrued" : 0.0, "calculationDate" : "2016-09-01"},
              "dueDate" : "2016-09-01"}]}""", OK),
      (
        """{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY",
             "debits": [{"originCode": "PAO1", "amount": 1000.00, "taxYearEnd": "2017-04-05",
              "interest": {"amountAccrued" : 0.0, "calculationDate" : "2016-09-01"},
              "dueDate" : "2016-09-01"}]}""", OK)
    )

    forAll(inputStatusData) { (input, statusCode) =>
      s"vary the status according to the input validity: input $input = $statusCode" in {
        val response = PaymentCalculationController.generate()(FakeRequest("POST", "/paymentschedule").withBody(Json.parse(input)))
        status(response) shouldBe statusCode
      }
    }

    "vary the payment schedule according to the input" in {
      val input =
        """{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "debits": [{"originCode": "PAO1", "amount": 1000.00,
              "interest" : { "amountAccrued": 0.0, "calculationDate" : "2016-09-01"},
              "dueDate" : "2016-09-01"}]}"""

      val response = PaymentCalculationController.generate()(FakeRequest("POST", "/paymentschedule").withBody(Json.parse(input)))
      val schedule = await(jsonBodyOf(response)).as[Seq[PaymentSchedule]]

      schedule.head.totalPayable.doubleValue() shouldBe (1002.27 +- 0.1)
    }

    val unhappyData = Table(
      ("input", "fieldName", "errorMessage"),
      ("""{ "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "debits": [{"originCode": "PAO1", "amount": 1000.00,
             "interest": {"amountAccrued" : 0.0, "calculationDate" : "2016-09-01" },
             "dueDate" : "2016-09-01"}]}""" , "initialPayment" , "initialPayment must not be null"),
      ("""{ "initialPayment" : 0.0, "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "debits": [{"originCode": "PAO1", "amount": 1000.00,
             "interest": {"amountAccrued" : 0.0, "calculationDate" : "2016-09-01" },
             "dueDate" : "2016-09-01"}]}""" , "startDate" , "startDate must not be null"),
      ("""{ "initialPayment" : 0.0, "startDate" : "2016-09-01",
             "paymentFrequency" : "MONTHLY", "debits": [{"originCode": "PAO1", "amount": 1000.00,
             "interest": {"amountAccrued" : 0.0, "calculationDate" : "2016-09-01" },
             "dueDate" : "2016-09-01"}]}""" , "endDate" , "endDate must not be null"),
      ("""{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "debits": [{"originCode": "PAO1", "amount": 1000.00,
             "interest": {"amountAccrued" : 0.0, "calculationDate" : "2016-09-01" },
             "dueDate" : "2016-09-01"}]}""" , "paymentFrequency" , "paymentFrequency must not be null"),
      ("""{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY"}""" , "debits" , "debits must not be null"),
      ("""{ "initialPayment" : -10.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "debits": [{"originCode": "PAO1", "amount": 1000.00,
              "amountAccrued" : 0.0,
              "calculationDate" : "2016-09-01", "dueDate" : "2016-09-01"}]}""" , "initialPayment" , "initialPayment must be a positive amount"),
      ("""{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "debits": []}""" , "debits" , "debits must contain at least one item"),
      ("""{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "debits": [{"originCode": "PAO1",
              "amountAccrued" : 0.0,
              "calculationDate" : "2016-09-01", "dueDate" : "2016-09-01"}]}""" , "debits[0].amount" , "liability amount must not be null"))

    forAll(unhappyData) { (input, fieldName, errorName) =>
      s"verify the unhappy path: input: $fieldName = $errorName" in {
        val response = PaymentCalculationController.generate()(FakeRequest("POST", "/paymentschedule").withHeaders(("Accepts", "application/json"), ("Content-Type", "application/json")).withBody(Json.parse(input)))

        status(response) shouldBe 400
      }
    }
  }
}
