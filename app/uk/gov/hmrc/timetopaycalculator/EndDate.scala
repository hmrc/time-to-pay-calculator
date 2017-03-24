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

package uk.gov.hmrc.timetopaycalculator

import java.time.{LocalDate => Day}

object EndDate {

  type Liability = (Day, BigDecimal)

  def ruleA(
    otherLiability: Iterable[Liability]
  ): Day = ???

  def ruleB(
    saDueDate: Day
  ): Day = endPrevMonth(saDueDate)

  def ruleC(
    ttpDebts: Iterable[Liability]
  ): Day = ttpDebts.collect{
    case (due, amt) if amt >= 32 => due
  }.min.plusYears(1)

  implicit def dayOrder: Ordering[Day] = Ordering.fromLessThan(_ isBefore _)

  private[timetopaycalculator] def endPrevMonth(in: Day): Day = {
    val prev = in.minusMonths(1)
    prev.plusDays(prev.lengthOfMonth - prev.getDayOfMonth)
  }

  def apply(
    saDueDate: Option[Day],
    ttpDebts: Iterable[Liability],
    otherLiability: Iterable[Liability] = Nil
  ): Day = List(
//    ruleA(otherLiability),
    saDueDate.map(ruleB),
    Some(ruleC(ttpDebts))
  ).flatten.min

  def monthlyIntervals(start: Day = Day.now): Stream[Day] = {
    import Math.min
    def inner(v: Day): Stream[Day] = v #:: inner {
      val i = v.plusMonths(1)
      i.plusDays{min(start.getDayOfMonth, i.lengthOfMonth) - i.getDayOfMonth}
    }
    inner(start)
  }

}
