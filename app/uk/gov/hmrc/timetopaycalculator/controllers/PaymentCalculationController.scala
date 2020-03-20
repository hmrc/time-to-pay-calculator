/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc._
import timetopaycalculator.cor.model.{CalculatorInput, PaymentSchedule}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.timetopaycalculator.services.CalculatorService

import scala.concurrent.Future

@Singleton
class PaymentCalculationController @Inject() (val calculatorService: CalculatorService, cc: ControllerComponents) extends BackendController(cc) {

  def generate() = Action.async(parse.json) { implicit request =>
    withJsonBody[CalculatorInput] { calculation =>
      val schedule: PaymentSchedule = calculatorService.buildSchedule(calculation)
      Future.successful(Ok(Json.toJson(schedule)))
    }
  }
}
