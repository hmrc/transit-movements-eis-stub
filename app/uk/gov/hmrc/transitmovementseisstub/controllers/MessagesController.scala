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
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.api.mvc.Headers
import play.api.mvc.Request
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.transitmovementseisstub.controllers.stream.StreamingParsers

import javax.inject.Inject

class MessagesController @Inject() (cc: ControllerComponents)(implicit val materializer: Materializer) extends BackendController(cc) with StreamingParsers {

  def channelResponseGB(): Action[Source[ByteString, _]] = Action(streamFromMemory) {
    request: Request[Source[ByteString, _]] =>
      request.body.runWith(Sink.ignore)

      validateHeaders(request.headers, "gb") match {
        case true => Ok
        case _    => BadRequest
      }
  }

  def channelResponseXI(): Action[Source[ByteString, _]] = Action(streamFromMemory) {
    request: Request[Source[ByteString, _]] =>
      request.body.runWith(Sink.ignore)

      validateHeaders(request.headers, "xi") match {
        case true => Ok
        case _    => BadRequest
      }
  }

  def validateHeaders(headers: Headers, officeCode: String): Boolean = {
    var result = isHeaderExists("content-type", headers) &&
      isHeaderExists("accept", headers) &&
      headers.get("authorization").nonEmpty &&
      headers.get("x-message-type").nonEmpty &&
      headers.get("x-correlation-id").nonEmpty &&
      headers.get("date").nonEmpty

    if (officeCode.equals("xi")) {
      result = headers.get("x-conversation-id").nonEmpty
    }

    result
  }

  def isHeaderExists(headerName: String, headers: Headers) =
    headers.get(headerName) match {
      case Some("application/xml") => true
      case _                       => false
    }

}
