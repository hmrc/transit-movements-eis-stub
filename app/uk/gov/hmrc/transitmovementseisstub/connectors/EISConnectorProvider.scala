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

package uk.gov.hmrc.transitmovementseisstub.connectors

import akka.stream.Materializer
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import com.google.inject.Singleton
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.transitmovementseisstub.config.AppConfig
import uk.gov.hmrc.transitmovementseisstub.models.CountryCode
import uk.gov.hmrc.transitmovementseisstub.models.CountryCode.GB
import uk.gov.hmrc.transitmovementseisstub.models.CountryCode.XI

import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[EISConnectorProviderImpl])
trait EISConnectorProvider {

  def getFor(countryCode: CountryCode): EISConnector = countryCode match {
    case GB => gb
    case XI => xi
  }

  def gb: EISConnector
  def xi: EISConnector

}

@Singleton // singleton as the message connectors need to be singletons for the circuit breakers.
class EISConnectorProviderImpl @Inject() (
  appConfig: AppConfig,
  httpClientV2: HttpClientV2
)(implicit ec: ExecutionContext, mat: Materializer)
    extends EISConnectorProvider {

  lazy val gb: EISConnector = new EISConnectorImpl("GB", appConfig.eisGb, appConfig.headerCarrierConfig, httpClientV2)
  lazy val xi: EISConnector = new EISConnectorImpl("XI", appConfig.eisXi, appConfig.headerCarrierConfig, httpClientV2)

}
