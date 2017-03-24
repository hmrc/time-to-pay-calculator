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

import java.time.{LocalDate => Day}
import play.api.mvc._
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.timetopaycalculator.EndDate

class EndDateController extends Controller {
  def apply(debtStrings: String, saDueDateString: Option[String]) = Action {
    val saDueDate = saDueDateString.filter(_.nonEmpty).map(Day.parse(_))
    val debts = debtStrings.split("/").map{_.split("@").toList}.map {
      case h :: t => (Day.parse(t.mkString), BigDecimal(h))
    }

    Ok(EndDate(saDueDate, debts).toString)
  }
}
