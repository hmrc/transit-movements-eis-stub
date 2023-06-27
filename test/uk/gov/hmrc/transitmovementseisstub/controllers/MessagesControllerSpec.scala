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

package uk.gov.hmrc.transitmovementseisstub.controllers

import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.HeaderNames
import play.api.http.Status.FORBIDDEN
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsJson
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.status
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transitmovementseisstub.base.TestActorSystem
import uk.gov.hmrc.transitmovementseisstub.config.AppConfig
import uk.gov.hmrc.transitmovementseisstub.connectors.EISConnector
import uk.gov.hmrc.transitmovementseisstub.connectors.EISConnectorProvider
import uk.gov.hmrc.transitmovementseisstub.connectors.errors.RoutingError

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import scala.concurrent.Future

class MessagesControllerSpec
    extends AnyWordSpec
    with Matchers
    with TestActorSystem
    with MockitoSugar
    with BeforeAndAfterEach
    with ScalaCheckDrivenPropertyChecks {

  private val HTTP_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH).withZone(ZoneOffset.UTC)

  private val appConfig                = mock[AppConfig]
  private val mockEisConnectorProvider = mock[EISConnectorProvider]
  private val controller               = new MessagesController(appConfig, stubControllerComponents(), mockEisConnectorProvider)

  override def beforeEach(): Unit =
    reset(appConfig)

  lazy val formattedDate       = s"${HTTP_DATE_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC))} UTC"
  lazy val brokenFormattedDate = s"${HTTP_DATE_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC))} Z"

  "POST /" should {

    "return 200 if all required headers are present and in the correct format without an enforced auth token" in {
      when(appConfig.enforceAuthToken).thenReturn(false)
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post("gb").url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> formattedDate,
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml",
            HeaderNames.ACCEPT        -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post("gb")(fakeRequest)
      status(result) shouldBe OK
    }

    "return 200 if all required headers are present and in the correct format with an enforced auth token" in {
      when(appConfig.enforceAuthToken).thenReturn(true)
      when(appConfig.authToken).thenReturn("abc")
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post("gb").url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> formattedDate,
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml",
            HeaderNames.ACCEPT        -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post("gb")(fakeRequest)
      status(result) shouldBe OK
    }

    "return 403 if a required header is missing" in {
      when(appConfig.enforceAuthToken).thenReturn(false)
      val fakeRequest = FakeRequest("POST", routes.MessagesController.post("gb").url)
      val result      = controller.post("gb")(fakeRequest)
      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> "FORBIDDEN",
        "message" -> "Error in request: Required header is missing: X-Correlation-Id"
      )
    }

    "return 403 if all required headers are present and in the correct format except X-Correlation-Id" in {
      when(appConfig.enforceAuthToken).thenReturn(false)
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post("gb").url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> "UUID.randomUUID().toString",
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> formattedDate,
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml",
            HeaderNames.ACCEPT        -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post("gb")(fakeRequest)
      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> "FORBIDDEN",
        "message" -> "Error in request: Error in header X-Correlation-Id: UUID.randomUUID().toString is not a UUID"
      )
    }

    "return 403 if all required headers are present and in the correct format except X-Conversation-Id" in {
      when(appConfig.enforceAuthToken).thenReturn(false)
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post("gb").url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> "UUID.randomUUID().toString",
            HeaderNames.DATE          -> formattedDate,
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml",
            HeaderNames.ACCEPT        -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post("gb")(fakeRequest)
      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> "FORBIDDEN",
        "message" -> "Error in request: Error in header X-Conversation-Id: UUID.randomUUID().toString is not a UUID"
      )
    }

    "return 403 if all required headers are present and in the correct format except Date" in forAll(
      Gen.oneOf(
        "HTTP_DATE_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC))",
        brokenFormattedDate
      )
    ) {
      date =>
        when(appConfig.enforceAuthToken).thenReturn(false)
        val fakeRequest = FakeRequest(
          "POST",
          routes.MessagesController.post("gb").url,
          FakeHeaders(
            Seq(
              "X-Correlation-Id"        -> UUID.randomUUID().toString,
              "X-Conversation-Id"       -> UUID.randomUUID().toString,
              HeaderNames.DATE          -> date,
              HeaderNames.AUTHORIZATION -> "Bearer abc",
              HeaderNames.CONTENT_TYPE  -> "application/xml",
              HeaderNames.ACCEPT        -> "application/xml"
            )
          ),
          Source.empty[ByteString]
        )
        val result = controller.post("gb")(fakeRequest)
        status(result) shouldBe FORBIDDEN
        contentAsJson(result) shouldBe Json.obj(
          "code"    -> "FORBIDDEN",
          "message" -> s"Error in request: Error in header Date: Expected date in RFC 7231 format, instead got $date"
        )
    }

    "return 403 if all required headers are present and in the correct format except Authorization" in {
      when(appConfig.enforceAuthToken).thenReturn(false)
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post("gb").url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> formattedDate,
            HeaderNames.AUTHORIZATION -> "NotBearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml",
            HeaderNames.ACCEPT        -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post("gb")(fakeRequest)
      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> "FORBIDDEN",
        "message" -> "Error in request: Error in header Authorization: Bearer token not in expected format"
      )
    }

    "return 403 if all required headers are present and in the correct format except Authorization when ensuring the token is specific and does not match" in {
      when(appConfig.enforceAuthToken).thenReturn(true)
      when(appConfig.authToken).thenReturn("easyas123")
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post("gb").url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> formattedDate,
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml",
            HeaderNames.ACCEPT        -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post("gb")(fakeRequest)
      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> "FORBIDDEN",
        "message" -> "Error in request: Error in header Authorization: Bearer token does not match expected token"
      )
    }

    "return 403 if all required headers are present and in the correct format except Content Type" in {
      when(appConfig.enforceAuthToken).thenReturn(false)
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post("gb").url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> formattedDate,
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/json",
            HeaderNames.ACCEPT        -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post("gb")(fakeRequest)
      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> "FORBIDDEN",
        "message" -> "Error in request: Error in header Content-Type: Expected application/xml, got application/json"
      )
    }

    "return 403 if all required headers are present and in the correct format except Accept" in {
      when(appConfig.enforceAuthToken).thenReturn(false)
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post("gb").url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> formattedDate,
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml",
            HeaderNames.ACCEPT        -> "application/json"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post("gb")(fakeRequest)
      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> "FORBIDDEN",
        "message" -> "Error in request: Error in header Accept: Expected application/xml, got application/json"
      )
    }

    "return 403 if all required headers are present and in the expected format for LRN" in {
      when(appConfig.enforceAuthToken).thenReturn(false)
      val lrn = "LRN99999999"
      val xmlRequestBody =
        s"<n1:TraderChannelSubmission><ncts:CC015C><TransitOperation><LRN>$lrn</LRN></TransitOperation></ncts:CC015C></n1:TraderChannelSubmission>"

      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post("gb").url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> formattedDate,
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml",
            HeaderNames.ACCEPT        -> "application/xml"
          )
        ),
        Source.single(ByteString(xmlRequestBody, StandardCharsets.UTF_8))
      )
      val result = controller.post("gb")(fakeRequest)
      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> "FORBIDDEN",
        "message" -> s"Error in request: The supplied LRN: $lrn has already been used by submitter: messageSender"
      )
    }

    val mockEISConnector = mock[EISConnector]

    "return 200 when client id is in allow list and enable proxy is true" in {
      when(appConfig.clientAllowList).thenReturn(Seq("XYZ"))
      when(appConfig.enableProxyMode).thenReturn(true)
      when(mockEisConnectorProvider.gb) thenReturn mockEISConnector

      when(
        mockEISConnector.post(any[Source[ByteString, _]])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(())))

      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post("gb").url,
        FakeHeaders(
          Seq(
            "X-Client-Id"             -> "XYZ",
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> formattedDate,
            HeaderNames.ACCEPT        -> "application/xml",
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post("gb")(fakeRequest)
      status(result) shouldBe OK
    }

    "implement stub logic when client not in allow list" in {
      when(appConfig.clientAllowList).thenReturn(Seq("XYZ"))
      when(appConfig.enableProxyMode).thenReturn(true)

      when(
        mockEISConnector.post(any[Source[ByteString, _]])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(RoutingError("error", FORBIDDEN))))

      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post("gb").url,
        FakeHeaders(
          Seq(
            "X-Client-Id"             -> "notInAllowList",
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> formattedDate,
            HeaderNames.ACCEPT        -> "application/xml",
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post("gb")(fakeRequest)
      status(result) shouldBe OK
    }

    "implement stub logic when enable proxy is false" in {
      when(appConfig.clientAllowList).thenReturn(Seq("XYZ"))
      when(appConfig.enableProxyMode).thenReturn(false)

      when(
        mockEISConnector.post(any[Source[ByteString, _]])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(RoutingError("error", FORBIDDEN))))

      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post("gb").url,
        FakeHeaders(
          Seq(
            "X-Client-Id"             -> "XYZ",
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> formattedDate,
            HeaderNames.ACCEPT        -> "application/xml",
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post("gb")(fakeRequest)
      status(result) shouldBe OK
    }
  }
}
