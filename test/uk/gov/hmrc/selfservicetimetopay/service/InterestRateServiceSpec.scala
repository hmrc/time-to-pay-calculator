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

package uk.gov.hmrc.selfservicetimetopay.service

import java.io.FileNotFoundException
import java.time.LocalDate
import java.time.format.DateTimeParseException

import org.scalatest.prop.TableDrivenPropertyChecks._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.services.InterestRateService

class InterestRateServiceSpec extends UnitSpec with WithFakeApplication {

  case class IRS(override val filename: String) extends InterestRateService

  "The InterestRateService" should {
    val exceptionProducingFiles = Table(
      ("file", "type"),
      ("/nullPointer-interestRates.csv", classOf[IndexOutOfBoundsException]),
      ("/dateError-interestRates.csv", classOf[DateTimeParseException]),
      ("/decimalError-interestRates.csv", classOf[NumberFormatException]),
      ("/invalid-fileName.csv", classOf[FileNotFoundException]))

    forAll(exceptionProducingFiles) { (name, exType) =>
      s"Throw a $exType for an input filename $name" in {
        try {
          IRS(name).getRateAt(LocalDate.now)
        } catch {
          case ex: Throwable => ex.getClass shouldBe exType
        }
      }
    }

    "contain 234 entries with the default rate file" in {
      InterestRateService.rates.size shouldBe 234
    }

    val dateChecks = Table(
      ("date", "rate"),
      (LocalDate.parse("2016-04-01"), 0.5),
      (LocalDate.parse("2016-08-04"), 0.25),
      (LocalDate.parse("2016-08-05"), 0.25),
      (LocalDate.parse("2016-08-03"), 0.5),
      (LocalDate.parse("1975-01-20"), 11.25),
      (LocalDate.parse("1975-01-27"), 11),
      (LocalDate.parse("1975-01-26"), 11.25),
      (LocalDate.parse("2016-10-04"), 0.25),
      (LocalDate.parse("1975-01-01"), null)
    )

    forAll(dateChecks) { (date, rate) =>
      s"return a rate of $rate for $date" in {
        InterestRateService.getRateAt(date).map(_.rate).orNull shouldBe rate
      }
    }

    val dateRanges = Table(
      ("startDate",                   "endDate",                      "periods"),
      (LocalDate.parse("2016-04-01"), LocalDate.parse("2016-04-01"),  1),
      (LocalDate.parse("2016-04-01"), LocalDate.parse("2016-10-01"),  2),
      (LocalDate.parse("2016-11-30"), LocalDate.parse("2017-01-01"),  2))

    forAll(dateRanges) { (startDate, endDate, periods) =>
      s"return $periods between $startDate and $endDate" in {
        InterestRateService.getRatesForPeriod(startDate, endDate).size shouldBe periods
      }
    }
  }
}
