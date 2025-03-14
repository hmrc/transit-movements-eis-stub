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

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.testkit.NoMaterializer
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.transitmovementseisstub.config.AppConfig
import uk.gov.hmrc.transitmovementseisstub.config.EISInstanceConfig

import java.time.Clock
import scala.concurrent.ExecutionContext.Implicits.global

class EISConnectorProviderSpec extends AnyFreeSpec with HttpClientV2Support with Matchers with ScalaFutures with BeforeAndAfterEach {

  implicit val materializer: Materializer = NoMaterializer

  val appConfig = mock[AppConfig]

  override def beforeEach(): Unit = {
    when(appConfig.eisGb).thenReturn(
      new EISInstanceConfig(
        "http",
        "localhost",
        1234,
        "/gb"
      )
    )

    when(appConfig.eisXi).thenReturn(
      new EISInstanceConfig(
        "http",
        "localhost",
        1234,
        "/xi"
      )
    )
  }

  override def afterEach(): Unit =
    reset(appConfig)

  "When creating the provider" - {

    "getting the GB connector will get the GB config" in {

      // Given this message connector
      val sut = new EISConnectorProviderImpl(appConfig, httpClientV2, Clock.systemUTC())

      // When we call the lazy val for GB
      sut.gb

      // Then the appConfig eisGb should have been called
      verify(appConfig, times(1)).eisGb

      // and that appConfig eisXi was not
      verify(appConfig, times(0)).eisXi
    }

    "getting the XI connector will get the XI config" in {

      // Given this message connector
      val sut = new EISConnectorProviderImpl(appConfig, httpClientV2, Clock.systemUTC())

      // When we call the lazy val for XI
      sut.xi

      // Then the appConfig eisXi should have been called
      verify(appConfig, times(1)).eisXi

      // and that appConfig eisGb was not
      verify(appConfig, times(0)).eisGb
    }

    "both connectors are not the same" in {

      // Given this message connector
      val sut = new EISConnectorProviderImpl(appConfig, httpClientV2, Clock.systemUTC())

      sut.gb must not be sut.xi
    }

  }

}
