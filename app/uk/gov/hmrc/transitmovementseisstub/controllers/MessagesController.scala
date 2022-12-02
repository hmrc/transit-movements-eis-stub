/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.http.MimeTypes
import play.api.libs.json.JsString
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.api.mvc.Headers
import play.api.mvc.Request
import play.api.mvc.Results
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.transitmovementseisstub.controllers.stream.StreamingParsers

import javax.inject.Inject

class MessagesController @Inject() (cc: ControllerComponents)(implicit val materializer: Materializer) extends BackendController(cc) with StreamingParsers {

  def channelResponseGB(): Action[Source[ByteString, _]] = Action(streamFromMemory) {
    request: Request[Source[ByteString, _]] =>
      request.body.runWith(Sink.ignore)

      validateHeaders(request.headers, "gb") match {
        case Right(_) => Ok
        case Left(missingHeaders) =>
          Results.BadRequest(JsString(s"Expected but did not receive the following headers: ${missingHeaders.mkString(", ")}"))
      }
  }

  def channelResponseXI(): Action[Source[ByteString, _]] = Action(streamFromMemory) {
    request: Request[Source[ByteString, _]] =>
      request.body.runWith(Sink.ignore)

      validateHeaders(request.headers, "xi") match {
        case Right(_) => Ok
        case Left(missingHeaders) =>
          Results.BadRequest(JsString(s"Expected but did not receive the following headers: ${missingHeaders.mkString(", ")}"))
      }
  }

  def validateHeaders(headers: Headers, officeCode: String): Either[Seq[String], Unit] = {
    val mandatoryHeaders = Seq("content-type", "accept", "authorization", "x-message-type", "x-correlation-id", "date", "x-conversation-id")

    val missingHeaders = mandatoryHeaders.filter {
      header =>
        header match {
          case header if header == "content-type" || header == "accept" => !headers.get(header).contains(MimeTypes.XML)
          case "x-conversation-id"                                      => if (officeCode.equals("xi")) headers.get("x-conversation-id").isEmpty else false
          case _                                                        => headers.get(header).isEmpty
        }
    }

    if (missingHeaders.isEmpty) Right(()) else Left(missingHeaders)
  }

}
