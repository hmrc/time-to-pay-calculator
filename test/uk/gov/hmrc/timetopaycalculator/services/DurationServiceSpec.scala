/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatest.prop.TableDrivenPropertyChecks._
import support.ITSpec

class DurationServiceSpec extends ITSpec {
  val service = new DurationService

  val periodCalculationData = Table(
    ("startDate", "endDate", "periods", "description"),
    (LocalDate.parse("2016-01-01"), LocalDate.parse("2016-02-28"), 2, "2 periods for 2 months exactly"),
    (LocalDate.parse("2016-01-01"), LocalDate.parse("2016-03-01"), 3, "3 periods for 2 months + 1 day"),
    (LocalDate.parse("2015-12-30"), LocalDate.parse("2016-03-01"), 3, "3 periods for 2 months + 2 days"),
    (LocalDate.parse("2016-01-01"), LocalDate.parse("2016-03-15"), 3, "3 periods for 2 months + 14 days"),
    (LocalDate.parse("2016-01-01"), LocalDate.parse("2016-03-31"), 3, "3 periods for 2 months + 30 days"),
    (LocalDate.parse("2016-01-01"), LocalDate.parse("2016-03-31"), 3, "4 periods for 3 months exactly"),
    (LocalDate.parse("2016-01-01"), LocalDate.parse("2016-04-01"), 4, "4 periods for 3 months + 1 day")
  )

  forAll(periodCalculationData) { (startDate, endDate, periods, description) =>
    s"The DurationService: return $periods for $startDate to $endDate ($description)" in {
      service.getRepaymentDates(startDate, endDate).size shouldBe periods
    }
  }

  "The DurationService: throw an exception if the startDate is AFTER the endDate" in {
    try {
      service.getRepaymentDates(LocalDate.parse("2017-01-01"), LocalDate.parse("2016-04-01"))
      fail("Exception should have been thrown")
    } catch {
      case _: IllegalArgumentException => None
      case ex: Throwable               => fail(s"Wrong exception thrown: ${ex.getClass}")
    }
  }

  val daysBetweenData = Table(
    ("startDate", "endDate", "count"),
    (LocalDate.parse("2016-01-01"), LocalDate.parse("2016-01-02"), 1),
    (LocalDate.parse("2016-01-01"), LocalDate.parse("2016-01-03"), 2),
    (LocalDate.parse("2016-01-01"), LocalDate.parse("2016-01-04"), 3),
    (LocalDate.parse("2015-01-01"), LocalDate.parse("2017-01-01"), 365 + 366),
    (LocalDate.parse("2017-01-01"), LocalDate.parse("2016-01-03"), 0)
  )

  forAll(daysBetweenData) { (startDate, endDate, count) =>
    s"The DurationService: $count days between $startDate and $endDate" in {
      service.getDaysBetween(startDate, endDate) shouldBe count
    }
  }

}
