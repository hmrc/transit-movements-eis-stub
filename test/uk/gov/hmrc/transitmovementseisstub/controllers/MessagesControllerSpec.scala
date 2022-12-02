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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.OK
import play.api.http.Status.BAD_REQUEST
import play.api.libs.json.JsString
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsJson
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.status
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.transitmovementseisstub.base.TestActorSystem

class MessagesControllerSpec extends AnyWordSpec with Matchers with TestActorSystem {

  private var fakeRequestGB = FakeRequest(
    method = "POST",
    uri = routes.MessagesController.channelResponseGB.url,
    headers = FakeHeaders(
      Seq(
        "content-type"     -> "application/xml",
        "accept"           -> "application/xml",
        "authorization"    -> "authorization",
        "x-message-type"   -> "message-type",
        "date"             -> "date",
        "x-correlation-id" -> "x-correlation-id"
      )
    ),
    body = AnyContentAsEmpty
  )

  private var fakeRequestXI = FakeRequest(
    method = "POST",
    uri = routes.MessagesController.channelResponseXI.url,
    headers = FakeHeaders(
      Seq(
        "content-type"      -> "application/xml",
        "accept"            -> "application/xml",
        "authorization"     -> "authorization",
        "x-message-type"    -> "message-type",
        "date"              -> "date",
        "x-correlation-id"  -> "x-correlation-id",
        "x-conversation-id" -> "x-conversation-id"
      )
    ),
    body = AnyContentAsEmpty
  )
  private val controller = new MessagesController(stubControllerComponents())

  "channelResponseGB POST /" should {
    "return 200" in {
      val result = controller.channelResponseGB()(fakeRequestGB)
      status(result) shouldBe OK
    }
    "return 400 - given headers missing" in {
      fakeRequestGB = FakeRequest("POST", routes.MessagesController.channelResponseGB.url)
      val result = controller.channelResponseGB()(fakeRequestGB)
      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe JsString(
        "Expected but did not receive the following headers: content-type, accept, authorization, x-message-type, x-correlation-id, date"
      )
    }
  }

  "channelResponseXI POST /" should {
    "return 200" in {
      val result = controller.channelResponseXI()(fakeRequestXI)
      status(result) shouldBe OK
    }
    "return 400 - given headers missing" in {
      fakeRequestXI = FakeRequest("POST", routes.MessagesController.channelResponseXI.url)
      val result = controller.channelResponseXI()(fakeRequestXI)
      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe JsString(
        "Expected but did not receive the following headers: content-type, accept, authorization, x-message-type, x-correlation-id, date, x-conversation-id"
      )
    }
  }
}
