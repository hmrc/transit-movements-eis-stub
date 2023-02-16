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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
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
import uk.gov.hmrc.transitmovementseisstub.base.TestActorSystem

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class MessagesControllerSpec extends AnyWordSpec with Matchers with TestActorSystem {

  private val HTTP_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneOffset.UTC)

  private val controller = new MessagesController(stubControllerComponents())

  "POST /" should {

    "return 200 if all required headers are present and in the correct format" in {
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post.url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> HTTP_DATE_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC)),
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml",
            HeaderNames.ACCEPT        -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post()(fakeRequest)
      status(result) shouldBe OK
    }

    "return 403 if a required header is missing" in {
      val fakeRequest = FakeRequest("POST", routes.MessagesController.post.url)
      val result      = controller.post()(fakeRequest)
      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> "FORBIDDEN",
        "message" -> "Error in request: Required header is missing: X-Correlation-Id"
      )
    }

    "return 403 if all required headers are present and in the correct format except X-Correlation-Id" in {
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post.url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> "UUID.randomUUID().toString",
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> HTTP_DATE_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC)),
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml",
            HeaderNames.ACCEPT        -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post()(fakeRequest)
      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> "FORBIDDEN",
        "message" -> "Error in request: Error in header X-Correlation-Id: UUID.randomUUID().toString is not a UUID"
      )
    }

    "return 403 if all required headers are present and in the correct format except X-Conversation-Id" in {
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post.url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> "UUID.randomUUID().toString",
            HeaderNames.DATE          -> HTTP_DATE_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC)),
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml",
            HeaderNames.ACCEPT        -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post()(fakeRequest)
      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> "FORBIDDEN",
        "message" -> "Error in request: Error in header X-Conversation-Id: UUID.randomUUID().toString is not a UUID"
      )
    }

    "return 403 if all required headers are present and in the correct format except Date" in {
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post.url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> "HTTP_DATE_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC))",
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml",
            HeaderNames.ACCEPT        -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post()(fakeRequest)
      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> "FORBIDDEN",
        "message" -> "Error in request: Error in header Date: Expected date in RFC 7231 format, instead got HTTP_DATE_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC))"
      )
    }

    "return 403 if all required headers are present and in the correct format except Authorization" in {
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post.url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> HTTP_DATE_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC)),
            HeaderNames.AUTHORIZATION -> "NotBearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml",
            HeaderNames.ACCEPT        -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post()(fakeRequest)
      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> "FORBIDDEN",
        "message" -> "Error in request: Error in header Authorization: Bearer token not in expected format"
      )
    }

    "return 403 if all required headers are present and in the correct format except Content Type" in {
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post.url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> HTTP_DATE_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC)),
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/json",
            HeaderNames.ACCEPT        -> "application/xml"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post()(fakeRequest)
      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> "FORBIDDEN",
        "message" -> "Error in request: Error in header Content-Type: Expected application/xml, got application/json"
      )
    }

    "return 403 if all required headers are present and in the correct format except Accept" in {
      val fakeRequest = FakeRequest(
        "POST",
        routes.MessagesController.post.url,
        FakeHeaders(
          Seq(
            "X-Correlation-Id"        -> UUID.randomUUID().toString,
            "X-Conversation-Id"       -> UUID.randomUUID().toString,
            HeaderNames.DATE          -> HTTP_DATE_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC)),
            HeaderNames.AUTHORIZATION -> "Bearer abc",
            HeaderNames.CONTENT_TYPE  -> "application/xml",
            HeaderNames.ACCEPT        -> "application/json"
          )
        ),
        Source.empty[ByteString]
      )
      val result = controller.post()(fakeRequest)
      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe Json.obj(
        "code"    -> "FORBIDDEN",
        "message" -> "Error in request: Error in header Accept: Expected application/xml, got application/json"
      )
    }
  }
}
