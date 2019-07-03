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

package uk.gov.hmrc.timetopaycalculator.models

import java.time.{LocalDate, Year}

case class Debit(originCode: Option[String]       = None,
                 amount:     BigDecimal,
                 interest:   Option[Interest],
                 dueDate:    LocalDate,
                 endDate:    Option[LocalDate]    = None,
                 rate:       Option[InterestRate] = None,
                 taxYearEnd: Option[LocalDate]    = None) {

  def historicDailyRate: BigDecimal = rate.map(_.rate).getOrElse(BigDecimal(0)) / BigDecimal(Year.of(dueDate.getYear).length()) / BigDecimal(100)
}

case class Interest(amountAccrued: BigDecimal, calculationDate: LocalDate)

object Interest {
  val none = Interest(0, LocalDate.now)
}
