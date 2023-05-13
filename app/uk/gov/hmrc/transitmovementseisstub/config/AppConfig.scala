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

package uk.gov.hmrc.transitmovementseisstub.config

import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration

@Singleton
class AppConfig @Inject() (config: Configuration) {

  lazy val appName: String = config.get[String]("appName")

  lazy val enforceAuthToken: Boolean = config.get[Boolean]("authorisation.enforce")
  lazy val authToken: String         = config.get[String]("authorisation.token")

  lazy val enableProxyMode: Boolean = config.get[Boolean]("enable-proxy-mode")

  lazy val clientAllowList: Seq[String] = config.get[Seq[String]]("client-allow-list")

  lazy val eisXi: EISInstanceConfig = EISInstanceConfig(
    "https",
    "if.ws.tt.hmrc.gov.uk",
    443,
    "/ncts5/traderchannelsubmissionsxi/v1"
  )
  // config.get[EISInstanceConfig]("microservice.services.eis.xi")
  lazy val eisGb: EISInstanceConfig = config.get[EISInstanceConfig]("microservice.services.eis.gb")
}
