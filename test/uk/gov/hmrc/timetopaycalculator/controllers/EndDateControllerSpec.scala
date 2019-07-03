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

import play.mvc.Http.Status.OK
import support.{ITSpec, TestConnector}

class EndDateControllerSpec extends ITSpec {

  val connector = fakeApplication().injector.instanceOf[TestConnector]

  "endDateController endDateController allow a valid request" in {
    val response = connector.enddate("enddate/10000@2017-01-01/15000@2018-02-01").futureValue
    response.status shouldBe OK
  }
}
