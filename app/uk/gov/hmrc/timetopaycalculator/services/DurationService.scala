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

package uk.gov.hmrc.timetopaycalculator.services

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit._

trait DurationService {
  def getDaysBetween(startDate: LocalDate, endDate: LocalDate): Long = calculatePeriod(startDate, endDate, DAYS, inclusive = false)

  def getRepaymentDates(startDate: LocalDate, endDate: LocalDate): Seq[LocalDate] = {
    if(startDate.isAfter(endDate)) {
      throw new IllegalArgumentException("Start date must be BEFORE end date")
    }

    Iterator.iterate(startDate)(_ plusMonths 1).takeWhile(_.compareTo(endDate) <= 0).toSeq
  }

  private def calculatePeriod(startDate: LocalDate, endDate: LocalDate, frequency: ChronoUnit, inclusive: Boolean): Long = {
    frequency.between(startDate, endDate) + (if (inclusive) 1 else 0) match {
      case c if c > 0 => c
      case _ => 0
    }
  }
}

object DurationService extends DurationService
