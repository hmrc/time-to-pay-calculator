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

package uk.gov.hmrc.selfservicetimetopay.services

import java.io.FileNotFoundException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import uk.gov.hmrc.selfservicetimetopay.models.InterestRate

import scala.io.Source

object InterestRateService extends InterestRateService {
  val filename: String = "/interestRates.csv"
  override val source = Source.fromInputStream(getClass.getResourceAsStream(filename))
}

trait InterestRateService {
  val source: Source
  val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE:dd MMM yyyy")

  lazy val rates: Seq[InterestRate] = streamInterestRates(source)

  private val interestRateConsumer = { (rates: Seq[InterestRate], line: String) =>
    line.split(",").toSeq match {
      case Seq(date, rate) =>
        val endDate: Option[LocalDate] = rates.lastOption.map(ir => ir.startDate.minusDays(1))
        rates :+ InterestRate(LocalDate.parse(date, DATE_TIME_FORMATTER), endDate, BigDecimal(rate))
      case _ => throw new IndexOutOfBoundsException()
    }
  }

  private def streamInterestRates(source: Source): Seq[InterestRate] = {
    try {
      source.getLines().foldLeft(Seq[InterestRate]())(interestRateConsumer)
    } catch {
      case e: NullPointerException => throw new FileNotFoundException(s"$source")
      case t: Throwable => throw t
    }
  }

  def getRateAt(date: LocalDate): Option[InterestRate] = {
    rates.find(rate => rate.startDate.compareTo(date) <= 0)
  }

  def getRatesForPeriod(startDate: LocalDate, endDate: LocalDate): Seq[InterestRate] = {
    rates.filter { interestRate =>
      (interestRate.startDate.compareTo(endDate) <= 0) &&
        (interestRate.endDate.getOrElse(LocalDate.MAX).compareTo(startDate) >= 0)
    }.flatMap { rate =>
      val startYear = Seq(rate.startDate.getYear, startDate.getYear).max
      val endYear = Seq(rate.endDate.getOrElse(LocalDate.MAX).getYear, endDate.getYear).min

      Range.inclusive(startYear, endYear).map { year =>
        InterestRate(LocalDate.of(year, 1, 1), Some(LocalDate.of(year, 12, 31)), rate.rate)
      }
    }
  }
}
