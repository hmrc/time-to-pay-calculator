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
import java.time.LocalDate._
import java.time.format.DateTimeParseException

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.io.Source

class InterestRateServiceSpec extends UnitSpec  with OneAppPerSuite{
  val InterestRateService = new InterestRateService
  case class IRS(override val filename: String) extends InterestRateService {
    override val source = Source.fromInputStream(getClass.getResourceAsStream(filename))
  }

  "The InterestRateService" should {
    val exceptionProducingFiles = Table(
      ("file", "type"),
      ("/nullPointer-interestRates.csv", classOf[IndexOutOfBoundsException]),
      ("/dateError-interestRates.csv", classOf[DateTimeParseException]),
      ("/decimalError-interestRates.csv", classOf[NumberFormatException]))

    forAll(exceptionProducingFiles) { (name, exType) =>
      val service = IRS(name)

      s"Throw a $exType for an input filename $name" in {
        try {
          service rateOn now
        } catch {
          case ex: Throwable => ex.getClass shouldBe exType
        }
      }
    }

    "contain 17 entries with the default rate file" in {
      InterestRateService.rates.size shouldBe 17
    }

    val dateChecks = Table(
      ("date", "rate"),
      (LocalDate.parse("2016-11-01"), 2.75),
      (LocalDate.parse("2016-04-01"), 3.0),
      (LocalDate.parse("2016-08-04"), 3.0),
      (LocalDate.parse("2016-08-05"), 3.0),
      (LocalDate.parse("2016-08-03"), 3.0),
      (LocalDate.parse("1975-01-01"), null)
    )

    forAll(dateChecks) { (date, rate) =>
      s"return a rate of $rate for $date" in {
        InterestRateService.rateOn(date).map(_.rate).orNull shouldBe rate
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
