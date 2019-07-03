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

package uk.gov.hmrc.timetopaycalculator

import java.time.LocalDate

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.min
import play.api.libs.json._

import scala.language.implicitConversions

package object models {
  implicit val localDateFormat: Format[LocalDate] = new Format[LocalDate] {
    override def reads(json: JsValue): JsResult[LocalDate] =
      json.validate[String].map(LocalDate.parse)

    override def writes(o: LocalDate): JsValue = Json.toJson(o.toString)
  }

  implicit val formatInterestRate: OFormat[InterestRate] = Json.format[InterestRate]
  implicit val formatInterest: OFormat[Interest] = Json.format[Interest]
  implicit val formatDebit: OFormat[Debit] = Json.format[Debit]

  def minSeqLength[T](length: Int)(implicit anySeqReads: Reads[Seq[T]]) = Reads[Seq[T]] { js =>
    anySeqReads.reads(js).filter(JsError(JsonValidationError("error.minLength", length)))(_.size >= length)
  }

  implicit val calculationReads: Reads[Calculation] = (
    (JsPath \ "debits").read[Seq[Debit]](minSeqLength[Debit](1)) and
    (JsPath \ "initialPayment").read[BigDecimal](min(BigDecimal(0))) and
    (JsPath \ "startDate").read[LocalDate] and
    (JsPath \ "endDate").read[LocalDate] and
    (JsPath \ "firstPaymentDate").readNullable[LocalDate] and
    (JsPath \ "paymentFrequency").read[String]
  ) (Calculation.apply _)

  implicit val formatInstalment: OFormat[Instalment] = Json.format[Instalment]
  implicit val formatPaymentSchedule: OFormat[PaymentSchedule] = Json.format[PaymentSchedule]
}
