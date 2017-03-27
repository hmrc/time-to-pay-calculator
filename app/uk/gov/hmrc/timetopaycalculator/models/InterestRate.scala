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

package uk.gov.hmrc.timetopaycalculator.models

import java.time.LocalDate
import java.time.LocalDate.{MAX, MIN}

object InterestRate {
  object NONE extends InterestRate(MIN, None, BigDecimal(0))
}

case class InterestRate(startDate: LocalDate, endDate: Option[LocalDate], rate: BigDecimal) {
  def dailyRate: BigDecimal = rate / yearLength

  def yearLength: BigDecimal = {
    val yrs = Range.inclusive(startDate.getYear, endDate.map(_.getYear).getOrElse(startDate.getYear))

    BigDecimal(yrs.map { yr =>
      LocalDate.of(yr, 1, 1).lengthOfYear()
    }.sum) / BigDecimal(yrs.size)
  }

  def containsDate(date: LocalDate): Boolean = date.compareTo(startDate) >= 0 && (date.compareTo(endDate.getOrElse(MAX)) <= 0)
}
