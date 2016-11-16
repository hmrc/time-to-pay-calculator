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

package uk.gov.hmrc.timetopaycalculator.models

import java.time.LocalDate

case class Debit(originCode: String, amount: BigDecimal, interest: Interest, dueDate: LocalDate, endDate: Option[LocalDate] = None, rate: Option[InterestRate] = None)

case class Interest(amountAccrued: BigDecimal, calculationDate: LocalDate)

object Debit {
  def partialOf(l: Debit, startDate: LocalDate, endDate: LocalDate, rate: InterestRate): Debit = {
    Debit(l.originCode, l.amount, l.interest, startDate, Some(endDate), Some(rate))
  }
}
