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

package uk.gov.hmrc.selfservicetimetopay.controllers

import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class PaymentControllerSpec extends UnitSpec with WithFakeApplication {

/*
  @Shared
  def fakeApplication = fakeApplication(new SsttpMicroserviceGlobal())

  def setupSpec() {
    start(fakeApplication)
  }

  def cleanupSpec() {
    stop(fakeApplication)
  }

  @Unroll
  def "Verify that the status varies according to the input validity: input #input = #statusCode"() {
    when:
    def response = route(new FakeRequest("POST", "/paymentschedule").withJsonBody(Json.parse(input)))

    then:
      status(response) == statusCode

    where:
      input                                                                                           ||  statusCode
    """{}"""                                                                                        ||  BAD_REQUEST
    """{ "initialPayment": 250.0, "startDate": "26-10-2015" }"""                                    ||  BAD_REQUEST
    """{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "liabilities": [{"type": "PAO1", "amount": 1000.00,
              "interestAccrued" : 0.0,
              "interestCalculationDate" : "2016-09-01", "dueDate" : "2016-09-01"}]}"""                  ||  OK
  }

  @Unroll
  def "Verify unhappy path: input #fieldName = #errorMessage"() {
    when:
    def response = contentAsString(route(new FakeRequest("POST", "/paymentschedule").withJsonBody(Json.parse(input))))
    def asJson = new JsonSlurper().parseText(response)

    then:
      asJson[fieldName][0] == errorMessage

    where:
      input                                                                                           ||  fieldName           |   errorMessage
    """{ "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "liabilities": [{"type": "PAO1", "amount": 1000.00,
              "interestAccrued" : 0.0,
              "interestCalculationDate" : "2016-09-01", "dueDate" : "2016-09-01"}]}"""                  ||  "initialPayment"    |   "initialPayment must not be null"
    """{ "initialPayment" : 0.0, "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "liabilities": [{"type": "PAO1", "amount": 1000.00,
              "interestAccrued" : 0.0,
              "interestCalculationDate" : "2016-09-01", "dueDate" : "2016-09-01"}]}"""                   ||  "startDate"        |   "startDate must not be null"
    """{ "initialPayment" : 0.0, "startDate" : "2016-09-01",
             "paymentFrequency" : "MONTHLY", "liabilities": [{"type": "PAO1", "amount": 1000.00,
              "interestAccrued" : 0.0,
              "interestCalculationDate" : "2016-09-01", "dueDate" : "2016-09-01"}]}"""                   ||  "endDate"          |   "endDate must not be null"
    """{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "liabilities": [{"type": "PAO1", "amount": 1000.00,
              "interestAccrued" : 0.0,
              "interestCalculationDate" : "2016-09-01", "dueDate" : "2016-09-01"}]}"""                   ||  "paymentFrequency" |   "paymentFrequency must not be null"
    """{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY"}"""                                                          ||  "liabilities"      |   "liabilities must not be null"
    """{ "initialPayment" : -10.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "liabilities": [{"type": "PAO1", "amount": 1000.00,
              "interestAccrued" : 0.0,
              "interestCalculationDate" : "2016-09-01", "dueDate" : "2016-09-01"}]}"""                   ||  "initialPayment"   |   "initialPayment must be a positive amount"
    """{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "liabilities": []}"""                                       ||  "liabilities"      |   "liabilities must contain at least one item"
    """{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "liabilities": [{"type": "PAO1",
              "interestAccrued" : 0.0,
              "interestCalculationDate" : "2016-09-01", "dueDate" : "2016-09-01"}]}"""                   ||  "liabilities[0].amount" |   "liability amount must not be null"
  }

  @Unroll
  def "Verify that the payment schedule varies according to the input validity: input #input = #paymentSchedule"() {
    when:
    def response = contentAsString(route(new FakeRequest("POST", "/paymentschedule").withJsonBody(Json.parse(input))))
    def asJson = new JsonSlurper().parseText(response)

    then:
      asJson["totalPayable"][0] == totalPayable

    where:
      input                                                                                           ||  totalPayable
    """{ "initialPayment" : 0.0, "startDate" : "2016-09-01", "endDate" : "2016-11-30",
             "paymentFrequency" : "MONTHLY", "liabilities": [{"type": "PAO1", "amount": 1000.00,
              "interestAccrued" : 0.0,
              "interestCalculationDate" : "2016-09-01", "dueDate" : "2016-09-01"}]}"""                   ||  1000.42
  }
*/
}
