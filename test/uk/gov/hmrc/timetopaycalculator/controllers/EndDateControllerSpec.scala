/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.mvc.Result
import play.api.test.FakeRequest
import play.mvc.Http.Status.OK

import scala.concurrent.Future
class EndDateControllerSpec extends Spec {

  val endDateController = new EndDateController

  "endDateController" should {

    "endDateController allow a valid request" in {

      val response: Future[Result] = endDateController.apply("10000@2017-01-01/15000@2018-02-01")(FakeRequest("GET", "/enddate/10000@2017-01-01/15000@2018-02-01"))
      status(response) shouldBe OK
    }
  }
}
