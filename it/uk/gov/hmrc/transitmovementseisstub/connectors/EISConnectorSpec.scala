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
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.mockito.ArgumentMatchers
import org.mockito.MockitoSugar
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.HeaderNames
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.transitmovementseisstub.base.TestActorSystem
import uk.gov.hmrc.transitmovementseisstub.config.EISInstanceConfig
import uk.gov.hmrc.transitmovementseisstub.config.Headers
import uk.gov.hmrc.transitmovementseisstub.connectors.errors.RoutingError
import uk.gov.hmrc.transitmovementseisstub.utils.FakeRequestBuilder
import uk.gov.hmrc.transitmovementseisstub.utils.WiremockSuite

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

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

  val uriStub = "/transit-movements-eis-stub/movements/messages"

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

  def stub(codeToReturn: Int) =
    server.stubFor(
      post(
        urlEqualTo(uriStub)
      )
        .inScenario("Standard Call")
        .withHeader(HeaderNames.ACCEPT, equalTo("application/xml"))
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo("application/xml"))
        .willReturn(aResponse().withStatus(codeToReturn))
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
      statusCode =>
        stub(INTERNAL_SERVER_ERROR)

        val hc = HeaderCarrier()

        whenReady(connector.post(source)(hc)) {
          case Left(x) =>
            x.statusCode mustBe statusCode
          case _ =>
            fail("Unexpected status code")
        }
    }
  }

}
