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
import play.mvc.Http.Status.{BAD_REQUEST, OK}

object Data {

  val goodStatusData = Table(
    ("input", "statusCode"),
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

  val badStatusData = Table(
    ("input", "statusCode"),
    ("{}", BAD_REQUEST),
    ("""{ "initialPayment": 250.0, "startDate": "26-10-2015" }""", BAD_REQUEST)
  )

  val unhappyData = Table(
    ("input", "fieldName", "errorMessage"),
    (
      """{ "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "debits": [{"originCode": "PAO1", "amount": 1000.00,
             "interest": {"amountAccrued" : 0.0, "calculationDate" : "2016-09-01" },
             "dueDate" : "2016-09-01"}]}""", "initialPayment", "initialPayment must not be null"),
    (
      """{ "initialPayment" : 0.0, "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "debits": [{"originCode": "PAO1", "amount": 1000.00,
             "interest": {"amountAccrued" : 0.0, "calculationDate" : "2016-09-01" },
             "dueDate" : "2016-09-01"}]}""", "startDate", "startDate must not be null"),
    (
      """{ "initialPayment" : 0.0, "startDate" : "2016-09-01",
             "paymentFrequency" : "MONTHLY", "debits": [{"originCode": "PAO1", "amount": 1000.00,
             "interest": {"amountAccrued" : 0.0, "calculationDate" : "2016-09-01" },
             "dueDate" : "2016-09-01"}]}""", "endDate", "endDate must not be null"),
    (
      """{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "debits": [{"originCode": "PAO1", "amount": 1000.00,
             "interest": {"amountAccrued" : 0.0, "calculationDate" : "2016-09-01" },
             "dueDate" : "2016-09-01"}]}""", "paymentFrequency", "paymentFrequency must not be null"),
    (
      """{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY"}""", "debits", "debits must not be null"),
    (
      """{ "initialPayment" : -10.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "debits": [{"originCode": "PAO1", "amount": 1000.00,
              "amountAccrued" : 0.0,
              "calculationDate" : "2016-09-01", "dueDate" : "2016-09-01"}]}""", "initialPayment", "initialPayment must be a positive amount"),
    (
      """{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "debits": []}""", "debits", "debits must contain at least one item"),
    (
      """{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "debits": [{"originCode": "PAO1",
              "amountAccrued" : 0.0,
              "calculationDate" : "2016-09-01", "dueDate" : "2016-09-01"}]}""", "debits[0].amount", "liability amount must not be null"))

}
