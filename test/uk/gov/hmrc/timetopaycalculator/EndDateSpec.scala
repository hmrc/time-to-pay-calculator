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

package uk.gov.hmrc.timetopaycalculator

import java.time.{LocalDate => Day}

import org.scalatest.prop.TableDrivenPropertyChecks._
import support.UnitSpec

class EndDateSpec extends UnitSpec {

  implicit class DateHelper(val sc: StringContext) {
    def date(args: Any*): Day = Day.parse(sc.parts.head)
  }

  val periodCalculationData = Table(
    ("customer", "ttp", "saDueDate", "expectedB", "expectedC"),
    (1, Seq((date"2017-01-31", 1000), (date"2017-01-31", 2000), (date"2017-07-31", 2000)), date"2018-01-31", date"2017-12-31", date"2018-01-31"),
    (2, Seq((date"2017-01-31", 1000), (date"2017-01-31", 3000), (date"2017-07-31", 3000)), date"2018-01-31", date"2017-12-31", date"2018-01-31"),
    (4, Seq((date"2017-01-31", 1000), (date"2017-01-31", 3000), (date"2017-07-31", 3000)), date"2018-01-31", date"2017-12-31", date"2018-01-31"),
    (9, Seq((date"2017-01-31", 0), (date"2017-01-31", 0), (date"2017-07-31", 5000)), date"2018-01-31", date"2017-12-31", date"2018-07-31"),
    (10, Seq((date"2017-01-31", 0), (date"2017-01-31", 0), (date"2017-07-31", 3000)), date"2018-01-31", date"2017-12-31", date"2018-07-31"),
    (11, Seq((date"2017-01-31", 0), (date"2017-01-31", 0), (date"2017-07-31", 3000)), date"2018-01-31", date"2017-12-31", date"2018-07-31"),
    (15, Seq((date"2017-07-31", 5000)), date"2019-01-31", date"2018-12-31", date"2018-07-31"),
    (16, Seq((date"2017-07-31", 5000), (date"2018-01-31", 2000)), date"2019-01-31", date"2018-12-31", date"2018-07-31"),
    (17, Seq((date"2017-07-31", 5000), (date"2018-01-31", 2000), (date"2018-01-31", 1000), (date"2018-07-31", 1000)), date"2019-01-31", date"2018-12-31", date"2018-07-31"),
    (19, Seq((date"2018-01-31", 1000), (date"2018-01-31", 2000), (date"2018-07-31", 3000)), date"2019-01-31", date"2018-12-31", date"2019-01-31"),
    (20, Seq((date"2018-01-31", 0), (date"2016-01-31", 0), (date"2017-04-28", 0), (date"2017-04-28", 1000)), date"2019-01-31", date"2018-12-31", date"2018-04-28"),
    (21, Seq((date"2016-01-31", 0), (date"2016-07-31", 0), (date"2017-01-31", 0), (date"2017-01-31", 0), (date"2017-04-28", 1000), (date"2017-07-31", 5000)), date"2018-01-31", date"2017-12-31", date"2018-04-28"),
    (22, Seq((date"2017-03-25", 100)), date"2018-01-31", date"2017-12-31", date"2018-03-25"),
    (24, Seq((date"2017-01-31", 500), (date"2017-01-31", 3000), (date"2017-01-31", 3000), (date"2017-03-25", 100)), date"2018-01-31", date"2017-12-31", date"2018-01-31"),
    (25, Seq((date"2016-12-18", 100), (date"2017-01-31", 50), (date"2017-01-31", 1100), (date"2017-07-31", 1100)), date"2018-01-31", date"2017-12-31", date"2017-12-18")
  )

  forAll(periodCalculationData) { (customer, ttp, saDueDate, expectedB, expectedC) =>
    s"Example customer $customer" should {
      s"calculate rule B as $expectedB (based on SA due date $saDueDate)" in {
        EndDate.ruleB(saDueDate) shouldBe (expectedB)
      }
      s"calculate rule C as $expectedC" in {
        EndDate.ruleC(ttp.map { case (a, b) => (a, BigDecimal(b)) }) shouldBe (expectedC)
      }

    }
  }

}
