/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.transitmovementseisstub.models

import uk.gov.hmrc.transitmovementseisstub.config.AppConfig

sealed trait CustomsOffice {
  def proxyEnabled(appConfig: AppConfig): Boolean
}

object CustomsOffice {

  case object Gb extends CustomsOffice {

    def proxyEnabled(appConfig: AppConfig): Boolean =
      appConfig.enableProxyModeGb
  }

  case object Xi extends CustomsOffice {

    def proxyEnabled(appConfig: AppConfig): Boolean =
      appConfig.enableProxyModeXi
  }

  case object Unknown extends CustomsOffice {
    def proxyEnabled(appConfig: AppConfig): Boolean = false
  }
}
