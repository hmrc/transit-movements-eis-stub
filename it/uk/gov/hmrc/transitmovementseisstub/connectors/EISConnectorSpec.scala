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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.mockito.ArgumentMatchers
import org.mockito.MockitoSugar
import org.scalacheck.Gen
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.HeaderNames
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.transitmovementseisstub.config.EISInstanceConfig
import uk.gov.hmrc.transitmovementseisstub.config.Headers
import uk.gov.hmrc.transitmovementseisstub.connectors.errors.RoutingError
import uk.gov.hmrc.transitmovementseisstub.utils.FakeRequestBuilder
import uk.gov.hmrc.transitmovementseisstub.utils.TestActorSystem
import uk.gov.hmrc.transitmovementseisstub.utils.TestHelpers
import uk.gov.hmrc.transitmovementseisstub.utils.WiremockSuite

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global

class EISConnectorSpec
    extends AnyWordSpec
    with HttpClientV2Support
    with Matchers
    with WiremockSuite
    with ScalaFutures
    with MockitoSugar
    with IntegrationPatience
    with ScalaCheckPropertyChecks
    with TableDrivenPropertyChecks
    with TestActorSystem {

  lazy val anything: StringValuePattern = new AnythingPattern()

  val uriStub = "/ncts5/traderchannelsubmissionsgb/v1"

  val connectorConfig: EISInstanceConfig = EISInstanceConfig(
    "http",
    "localhost",
    wiremockPort,
    uriStub,
    Headers("bearertokenhereGB")
  )

  // We construct the connector each time to avoid issues with the circuit breaker
  def connector = new EISConnectorImpl("eis", connectorConfig, httpClientV2)

  def source: Source[ByteString, _] = Source.single(ByteString.fromString("<test></test>"))

  def stub(codeToReturn: Int, body: String = "") =
    server.stubFor(
      post(
        urlEqualTo(uriStub)
      )
        .willReturn(aResponse().withStatus(codeToReturn).withBody(body))
    )

  "post" should {

    "returns a Right of unit if the call is successful" in {
      server.resetAll()

      stub(OK)

      val hc = HeaderCarrier()

      whenReady(connector.post(source)(hc)) {
        _.isRight mustBe true
      }
    }

    val errorCodes = Gen.oneOf(
      Seq(
        BAD_REQUEST,
        FORBIDDEN,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        GATEWAY_TIMEOUT
      )
    )

    "pass through error status codes" in forAll(errorCodes) {
      code =>
        stub(code, "error")

        val hc = HeaderCarrier()

        whenReady(connector.post(source)(hc)) {
          case Left(x) =>
            x.statusCode mustBe code
          case _ =>
            fail("Unexpected status code")
        }
    }

    "handle exceptions by returning an HttpResponse with status code 500" in {
      val httpClientV2 = mock[HttpClientV2]

      val hc        = HeaderCarrier()
      val connector = new EISConnectorImpl("Failure", connectorConfig, httpClientV2)

      when(httpClientV2.post(ArgumentMatchers.any[URL])(ArgumentMatchers.any[HeaderCarrier])).thenReturn(new FakeRequestBuilder)

      whenReady(connector.post(source)(hc)) {
        case Left(x) if x.isInstanceOf[RoutingError] => x.statusCode mustBe INTERNAL_SERVER_ERROR
        case _                                       => fail("Left was not a RoutingError")
      }
    }
  }

}
