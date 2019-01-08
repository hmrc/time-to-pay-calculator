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

import java.time.{LocalDate => Day}
import play.api.mvc._
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.timetopaycalculator.EndDate

/**
  * Provides calculation of the maximum end date for the TTP
  * duration in ISO-8601 format (e.g. 2018-04-01).
  * 
  * Debts should be supplied as separate elements of the path in
  * AMOUNT@DUEDATE format. 
  * 
  * {{{
  * GET /enddate/10000@2017-01-01/15000@2018-02-01?saDue=2018-04-01
  * }}}
  */
class EndDateController extends Controller {
  //todo invesigate why we never used this is the frontend ???????
  /**
    * @param debtStrings A '/' separated list of AMOUNT@DUEDATE values
    * @param saDueDateString The due date for the next Self Assessment
    *    return. If supplied this will cause 'rule B' to be applied in
    *    addition to 'rule A'
    */
  def apply(debtStrings: String, saDueDateString: Option[String] = None) = Action {
    val saDueDate = saDueDateString.filter(_.nonEmpty).map(Day.parse(_))
    val debts = debtStrings.split("/").map{_.split("@").toList}.map {
      case h :: t => (Day.parse(t.mkString), BigDecimal(h))
    }

    Ok(EndDate(saDueDate, debts).toString)
  }
}
