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

package uk.gov.hmrc.timetopaycalculator.models

import java.time.LocalDate

case class PaymentSchedule(startDate: LocalDate,
                           endDate: LocalDate,
                           initialPayment: BigDecimal,
                           amountToPay: BigDecimal,
                           instalmentBalance: BigDecimal,
                           totalInterestCharged: BigDecimal,
                           totalPayable: BigDecimal,
                           instalments: Seq[Instalment]) {
  override def toString: String =
    s"""
       |\tStart Date:         $startDate
       |\tEnd Date:           $endDate
       |
       |\tInitial Debit:      $amountToPay
       |\tInitial payment:    $initialPayment
       |\tTotal interest:     $totalInterestCharged
       |\tTotal payable:      $totalPayable
       |\tInstalment balance: $instalmentBalance
       |
       |\tInstalment count:   ${instalments.size}
       |
       |\tInstalments:
       |\t\t$instalmentsToString
     """.stripMargin

  def instalmentsToString: String = {
    instalments.map(_.toString).reduce { (acc, instalment) =>
      s"""$acc\n\t\t$instalment"""
    }
  }
}

case class Instalment(paymentDate: LocalDate, amount: BigDecimal, interest: BigDecimal) {
  override def toString: String =
    s"""$paymentDate: $amount ($interest)"""
}