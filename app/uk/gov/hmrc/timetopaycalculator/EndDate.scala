/*
 * Copyright 2018 HM Revenue & Customs
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

/**
  * Calculates the maximum permissible end date for a repayment
  * period. Within this window of time a monthly schedule will be
  * calculated. The actual end date will be either on this date, or
  * before it (for example if the maximum end date is the 26th of July
  * and the payments are to be made on the 12th of each month then the
  * last actual payment would be on the 12th on July). 
  * 
  * @see [[https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?pageId=76500139]]
  */
object EndDate {

  type Liability = (Day, BigDecimal)

  /**
    * End of the calendar month before the due date of the next
    * liability which is outside of the TTP. Not currently
    * implemented. 
    */
  def ruleA(
    otherLiability: Iterable[Liability]
  ): Day = ???

  /**
    * End of the calendar month before the due date of the next
    * non-submitted SA return
    */
  def ruleB(
    saDueDate: Day
  ): Day = endPrevMonth(saDueDate)

  /**
    * 12 months from the earliest due date of the amounts included in
    * the TTP, ignoring due dates for any amounts under £32
    */
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

  /**
    * Returns the earliest date from the applicable rules. 
    * 
    * @param saDueDate Day the next Self Assessment return is due
    *   on. If supplied this will cause Rule B to be applied. 
    * @param ttpDebts Amounts owed via TTP and the days the debts are
    *   due.
    * @param otherLiability Intended for Rule A, not currently used.
    */
  def apply(
    saDueDate: Option[Day],
    ttpDebts: Iterable[Liability],
    otherLiability: Iterable[Liability] = Nil
  ): Day = List(
//    ruleA(otherLiability),
    saDueDate.map(ruleB),
    Some(ruleC(ttpDebts))
  ).flatten.min

  /**
    * Provides a stream of LocalDate's on a fixed day of the
    * month, clipping to the last day of the month if necessary. 
    * 
    * {{{
    * val endOfJan = LocalDate.parse("2016-01-31")
    * val dates = EndDate.monthlyIntervals(endOfJan)
    * dates: Stream(2016-01-31, 2016-02-29, 2016-03-31, ...)
    * }}}
    */
  def monthlyIntervals(start: Day = Day.now): Stream[Day] = {
    import Math.min
    def inner(v: Day): Stream[Day] = v #:: inner {
      val i = v.plusMonths(1)
      i.plusDays{min(start.getDayOfMonth, i.lengthOfMonth) - i.getDayOfMonth}
    }
    inner(start)
  }

}
