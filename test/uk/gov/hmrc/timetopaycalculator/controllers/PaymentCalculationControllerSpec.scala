/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Logger
import play.api.libs.json.Json
import play.mvc.Http.Status._
import support.{ITSpec, TestConnector}
import uk.gov.hmrc.timetopaycalculator.models.PaymentSchedule

class PaymentCalculationControllerSpec extends ITSpec {

  val connector = fakeApplication().injector.instanceOf[TestConnector]

  forAll(Data.unhappyData) { (input, fieldName, errorName) =>
    s"The payment controller should verify the unhappy path: input: $fieldName = $errorName" in {
      val response = connector.paymentscheduleWithHeader(Json.parse(input)).failed.futureValue
      response.getMessage should include("returned 400")
    }
  }

  forAll(Data.goodStatusData) { (input, statusCode) =>
    s"The payment controller should vary the status according to the input validity: input $input = $statusCode" in {
      val response = connector.paymentschedule(Json.parse(input)).futureValue
      response.status shouldBe statusCode
    }
  }

  forAll(Data.badStatusData) { (input, statusCode) =>
    s"The payment controller should fail according to the input validity: input $input = $statusCode" in {
      val response = connector.paymentschedule(Json.parse(input)).failed.futureValue
      response.getMessage should include("returned 400")
    }
  }

  "the payment controller should vary the payment schedule according to the input" in {
    val input =
      """{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
               "paymentFrequency" : "MONTHLY", "debits": [{"originCode": "PAO1", "amount": 1000.00,
                "interest" : { "amountAccrued": 0.0, "calculationDate" : "2016-09-01"},
                "dueDate" : "2016-09-01"}]}"""

    val response = connector.paymentschedule(Json.parse(input)).futureValue
    val schedule = Json.parse(response.body).as[Seq[PaymentSchedule]]

    schedule.head.totalPayable.doubleValue() shouldBe (1002.27 +- 0.1)
  }

}
