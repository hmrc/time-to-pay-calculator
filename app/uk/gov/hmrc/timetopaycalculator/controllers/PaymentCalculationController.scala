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

import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.timetopaycalculator.models._
import uk.gov.hmrc.timetopaycalculator.services.{CalculatorService, DurationService, InterestRateService}

import scala.concurrent.Future

trait PaymentCalculationController extends BaseController {

  val calculatorService: CalculatorService

  def generate() = Action.async(parse.json) { implicit request =>
    withJsonBody[Calculation] { calculation =>
      Future.successful(Ok(Json.toJson(calculatorService.generateMultipleSchedules(calculation))))
    }
  }
}

object PaymentCalculationController extends PaymentCalculationController {
  override val calculatorService = new CalculatorService(InterestRateService, DurationService)
}
