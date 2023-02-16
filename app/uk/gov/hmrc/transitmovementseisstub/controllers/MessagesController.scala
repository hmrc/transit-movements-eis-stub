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

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Logging
import play.api.http.HeaderNames
import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderNames => HMRCHeaderNames}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.transitmovementseisstub.controllers.stream.StreamingParsers

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import scala.util.Failure
import scala.util.Success
import scala.util.Try

class MessagesController @Inject() (cc: ControllerComponents)(implicit val materializer: Materializer)
    extends BackendController(cc)
    with StreamingParsers
    with Logging {

  private val HTTP_DATE_FORMATTER  = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneOffset.UTC)
  private val UUID_PATTERN         = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$".r
  private val BEARER_TOKEN_PATTERN = "^Bearer \\S+$".r

  def post(): Action[Source[ByteString, _]] = Action(streamFromMemory) {
    implicit request: Request[Source[ByteString, _]] =>
      request.body.runWith(Sink.ignore)

      (for {
        _ <- validateHeader("X-Correlation-Id", isUuid)
        _ <- validateHeader("X-Conversation-Id", isUuid)
        _ <- validateHeader(HeaderNames.CONTENT_TYPE, isXml)
        _ <- validateHeader(HeaderNames.ACCEPT, isXml)
        _ <- validateHeader(HeaderNames.AUTHORIZATION, isBearerToken)
        _ <- validateHeader(HeaderNames.DATE, isDate)
      } yield ()) match {
        case Right(()) => Ok
        case Left(error) =>
          logger.error(s"""Request failed with the following error:
               |$error
               |
               |Request ID: ${request.headers.get(HMRCHeaderNames.xRequestId).getOrElse("unknown")}
               |""".stripMargin)
          Status(FORBIDDEN)(Json.obj("code" -> "FORBIDDEN", "message" -> s"Error in request: $error"))
      }
  }

  private def validateHeader(header: String, validation: String => Option[String])(implicit request: Request[_]): Either[String, Unit] =
    request.headers.get(header) match {
      case Some(value) =>
        validation(value)
          .map(
            result => s"Error in header $header: $result"
          )
          .toLeft(())
      case None => Left(s"Required header is missing: $header")
    }

  private def isUuid(value: String): Option[String] = if (UUID_PATTERN.matches(value)) None else Some(s"$value is not a UUID")

  private def isXml(value: String): Option[String] = if (value == MimeTypes.XML) None else Some(s"Expected application/xml, got $value")

  private def isBearerToken(value: String): Option[String] = if (BEARER_TOKEN_PATTERN.matches(value)) None else Some("Bearer token not in expected format")

  private def isDate(value: String): Option[String] =
    Try(HTTP_DATE_FORMATTER.parse(value)) match {
      case Success(_) => None
      case Failure(_) => Some(s"Expected date in RFC 7231 format, instead got $value")
    }

}
