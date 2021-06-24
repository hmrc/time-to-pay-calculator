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

package timetopaytaxpayer.cor

import timetopaycalculator.cor.model.{CalculatorInput, PaymentSchedule}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class CalculatorConnector(
    servicesConfig: ServicesConfig,
    http:           HttpClient
)(
    implicit
    ec: ExecutionContext
) {
  import uk.gov.hmrc.http.HttpReads.Implicits._

  lazy val baseUrl: String = servicesConfig.baseUrl("time-to-pay-calculator")

  def calculatePaymentschedule(calculatorInput: CalculatorInput, baseUrl: String = baseUrl)(implicit hc: HeaderCarrier): Future[PaymentSchedule] = {
    http.POST[CalculatorInput, PaymentSchedule](s"$baseUrl/time-to-pay-calculator/paymentschedule", calculatorInput)
  }
}
