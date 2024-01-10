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

import cats.data.EitherT
import cats.implicits.catsStdInstancesForFuture
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
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
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.status
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transitmovementseisstub.base.TestActorSystem
import uk.gov.hmrc.transitmovementseisstub.config.AppConfig
import uk.gov.hmrc.transitmovementseisstub.connectors.EISConnector
import uk.gov.hmrc.transitmovementseisstub.connectors.EISConnectorProvider
import uk.gov.hmrc.transitmovementseisstub.connectors.errors.RoutingError
import uk.gov.hmrc.transitmovementseisstub.models.CustomsOffice
import uk.gov.hmrc.transitmovementseisstub.models.LocalReferenceNumber
import uk.gov.hmrc.transitmovementseisstub.models.MessageSender
import uk.gov.hmrc.transitmovementseisstub.models.errors.ParserError
import uk.gov.hmrc.transitmovementseisstub.services.LRNExtractorServiceImpl

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import scala.xml.XML


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
  private val mockLrnExtractor         = mock[LRNExtractorServiceImpl]
  private val controller               = new MessagesController(appConfig, stubControllerComponents(), mockEisConnectorProvider, mockLrnExtractor)

  override def beforeEach(): Unit = {
    reset(appConfig)
    reset(mockEisConnectorProvider)
  }

  lazy val formattedDate       = s"${HTTP_DATE_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC))} UTC"
  lazy val brokenFormattedDate = s"${HTTP_DATE_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC))} Z"

  "POST /" should {

    "return 200 if all required headers are present and in the correct format without an enforced auth token" in {
      when(appConfig.enforceAuthToken).thenReturn(false)
      when(mockLrnExtractor.extractLRN(Source.empty[ByteString]))
        .thenReturn(EitherT.rightT[Future, ParserError]((LocalReferenceNumber("1234567"), MessageSender("abc"))))

      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post(CustomsOffice.Gb).url,
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
      val result = controller.post(CustomsOffice.Gb)(fakeRequest)
      status(result) shouldBe OK
    }

    "return 200 if all required headers are present and in the correct format with an enforced auth token" in {
      when(appConfig.enforceAuthToken).thenReturn(true)
      when(appConfig.authToken).thenReturn("abc")
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post(CustomsOffice.Gb).url,
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
      val result = controller.post(CustomsOffice.Gb)(fakeRequest)
      status(result) shouldBe OK
    }

    "return 403 if a required header is missing" in {
      when(appConfig.enforceAuthToken).thenReturn(false)
      val fakeRequest = FakeRequest("POST", routes.MessagesController.post(CustomsOffice.Gb).url)
      val result      = controller.post(CustomsOffice.Gb)(fakeRequest)
      status(result) shouldBe FORBIDDEN
    }

    "return 403 if all required headers are present and in the correct format except X-Correlation-Id" in {
      when(appConfig.enforceAuthToken).thenReturn(false)
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post(CustomsOffice.Gb).url,
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
      val result = controller.post(CustomsOffice.Gb)(fakeRequest)
      status(result) shouldBe FORBIDDEN
    }

    "return 403 if all required headers are present and in the correct format except X-Conversation-Id" in {
      when(appConfig.enforceAuthToken).thenReturn(false)
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post(CustomsOffice.Gb).url,
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
      val result = controller.post(CustomsOffice.Gb)(fakeRequest)
      status(result) shouldBe FORBIDDEN
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
          routes.MessagesController.post(CustomsOffice.Gb).url,
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
        val result = controller.post(CustomsOffice.Gb)(fakeRequest)
        status(result) shouldBe FORBIDDEN
    }

    "return 403 if all required headers are present and in the correct format except Authorization" in {
      when(appConfig.enforceAuthToken).thenReturn(false)
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post(CustomsOffice.Gb).url,
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
      val result = controller.post(CustomsOffice.Gb)(fakeRequest)
      status(result) shouldBe FORBIDDEN
    }

    "return 403 if all required headers are present and in the correct format except Authorization when ensuring the token is specific and does not match" in {
      when(appConfig.enforceAuthToken).thenReturn(true)
      when(appConfig.authToken).thenReturn("easyas123")
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post(CustomsOffice.Gb).url,
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
      val result = controller.post(CustomsOffice.Gb)(fakeRequest)
      status(result) shouldBe FORBIDDEN
    }

    "return 403 if all required headers are present and in the correct format except Content Type" in {
      when(appConfig.enforceAuthToken).thenReturn(false)
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post(CustomsOffice.Gb).url,
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
      val result = controller.post(CustomsOffice.Gb)(fakeRequest)
      status(result) shouldBe FORBIDDEN
    }

    "return 403 if all required headers are present and in the correct format except Accept" in {
      when(appConfig.enforceAuthToken).thenReturn(false)
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post(CustomsOffice.Gb).url,
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
      val result = controller.post(CustomsOffice.Gb)(fakeRequest)
      status(result) shouldBe FORBIDDEN
    }

    "return 403 if all required headers are present and in the expected format for duplicate LRN error" in {
      when(appConfig.enforceAuthToken).thenReturn(false)

      val lrn = "DUPLRN99999999"
      val xmlRequestBody =
        s"<TraderChannelSubmission><ncts:CC015C PhaseID='NCTS5.0' xmlns:ncts='http://ncts.dgtaxud.ec'><messageSender>messagesender</messageSender><TransitOperation><LRN>DUPLRN99999999</LRN></TransitOperation></ncts:CC015C></TraderChannelSubmission>"

      val requestBody = Source.single(ByteString(xmlRequestBody, StandardCharsets.UTF_8))
      when(mockLrnExtractor.extractLRN(requestBody))
        .thenReturn(EitherT.rightT[Future, ParserError]((LocalReferenceNumber(lrn), MessageSender("messagesender"))))

      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post(CustomsOffice.Gb).url,
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
        requestBody
      )
      val result = controller.post(CustomsOffice.Gb)(fakeRequest)
      status(result) shouldBe FORBIDDEN
      val resultXml = XML.loadString(contentAsString(result))
      (resultXml \ "sourceFaultDetail" \ "lrn").head.text shouldBe "DUPLRN99999999"
      (resultXml \ "sourceFaultDetail" \ "submitter").head.text shouldBe "messagesender"
    }

    val mockEISConnector = mock[EISConnector]

    "return 200 when client id is in allow list and enable proxy for GB is true" in {
      when(appConfig.clientAllowList).thenReturn(Seq("XYZ"))
      when(appConfig.internalAllowList).thenReturn(Seq.empty)
      when(appConfig.enableProxyModeGb).thenReturn(true)
      when(mockEisConnectorProvider.gb) thenReturn mockEISConnector

      when(
        mockEISConnector.post(any[Source[ByteString, _]])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(())))

      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post(CustomsOffice.Gb).url,
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
      val result = controller.post(CustomsOffice.Gb)(fakeRequest)
      status(result) shouldBe OK
      verify(mockEisConnectorProvider, times(0)).xi
      verify(mockEisConnectorProvider, times(1)).gb
    }

    "implement stub logic when client not in allow list" in {
      when(appConfig.clientAllowList).thenReturn(Seq("XYZ"))
      when(mockLrnExtractor.extractLRN(Source.empty[ByteString]))
        .thenReturn(EitherT.rightT[Future, ParserError]((LocalReferenceNumber("1234567"), MessageSender("abc"))))
      when(appConfig.internalAllowList).thenReturn(Seq.empty)
      when(appConfig.enableProxyModeGb).thenReturn(true)

      when(
        mockEISConnector.post(any[Source[ByteString, _]])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(RoutingError("error", FORBIDDEN))))

      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post(CustomsOffice.Gb).url,
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
      val result = controller.post(CustomsOffice.Gb)(fakeRequest)
      status(result) shouldBe OK
      verify(mockEisConnectorProvider, times(0)).xi
      verify(mockEisConnectorProvider, times(0)).gb
    }

    "implement stub logic when enable proxy is false" in {
      when(appConfig.clientAllowList).thenReturn(Seq("XYZ"))
      when(mockLrnExtractor.extractLRN(Source.empty[ByteString]))
        .thenReturn(EitherT.rightT[Future, ParserError]((LocalReferenceNumber("1234567"), MessageSender("abc"))))
      when(appConfig.internalAllowList).thenReturn(Seq.empty)
      when(appConfig.enableProxyModeGb).thenReturn(false)
      when(appConfig.enableProxyModeXi).thenReturn(true)

      when(
        mockEISConnector.post(any[Source[ByteString, _]])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(RoutingError("error", FORBIDDEN))))

      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post(CustomsOffice.Gb).url,
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
      val result = controller.post(CustomsOffice.Gb)(fakeRequest)
      status(result) shouldBe OK
      verify(mockEisConnectorProvider, times(0)).xi
      verify(mockEisConnectorProvider, times(0)).gb
    }

    "return 200 when client id is in allow list and enable proxy for XI is true" in {
      when(appConfig.clientAllowList).thenReturn(Seq("XYZ"))
      when(appConfig.internalAllowList).thenReturn(Seq.empty)
      when(appConfig.enableProxyModeGb).thenReturn(false)
      when(appConfig.enableProxyModeXi).thenReturn(true)
      when(mockEisConnectorProvider.xi) thenReturn mockEISConnector

      when(
        mockEISConnector.post(any[Source[ByteString, _]])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(())))

      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post(CustomsOffice.Xi).url,
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
      val result = controller.post(CustomsOffice.Xi)(fakeRequest)
      status(result) shouldBe OK
      verify(mockEisConnectorProvider, times(1)).xi
      verify(mockEisConnectorProvider, times(0)).gb
    }

    "return 200 when client id is in internal allow list and enable proxy for GB is false" in {
      when(appConfig.internalAllowList).thenReturn(Seq("XYZ"))
      when(appConfig.clientAllowList).thenReturn(Seq())
      when(appConfig.enableProxyModeGb).thenReturn(false)
      when(mockEisConnectorProvider.gb) thenReturn mockEISConnector

      when(
        mockEISConnector.post(any[Source[ByteString, _]])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(())))

      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post(CustomsOffice.Gb).url,
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
      val result = controller.post(CustomsOffice.Gb)(fakeRequest)
      status(result) shouldBe OK
      verify(mockEisConnectorProvider, times(0)).xi
      verify(mockEisConnectorProvider, times(1)).gb
    }

  }
}
