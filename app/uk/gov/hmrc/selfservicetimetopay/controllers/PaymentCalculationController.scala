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

import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import play.api.mvc._
import uk.gov.hmrc.selfservicetimetopay.models.Calculation

import scala.concurrent.Future

object PaymentCalculationController extends PaymentCalculationController

trait PaymentCalculationController extends BaseController {

	val calculationService:CalculationService = ???

	def generate() = Action.async { implicit request =>
		withJsonBody[Calculation] { calculation =>
			Future.successful(Ok(calculationService.generateMultipleSchedules(calculation)))
		}
	}
}
