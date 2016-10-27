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

package uk.gov.hmrc.selfservicetimetopay.models

import java.time.LocalDate

case class Calculation(liabilities: Seq[Liability], initialPayment: BigDecimal, startDate: LocalDate, endDate: LocalDate, paymentFrequency: String) {
  var initialPaymentRemaining: BigDecimal = 0

  def applyInitialPaymentToDebt(debtAmount: BigDecimal): BigDecimal = debtAmount match {
    case amt if amt <= initialPaymentRemaining =>  initialPaymentRemaining = initialPaymentRemaining - debtAmount; 0
    case amt => val remainingDebt = amt - initialPaymentRemaining; initialPaymentRemaining = 0; remainingDebt
  }
}
