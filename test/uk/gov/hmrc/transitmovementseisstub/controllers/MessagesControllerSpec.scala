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

import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.FakeRequest
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.status
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.transitmovementseisstub.base.TestActorSystem

class MessagesControllerSpec extends AnyWordSpec with Matchers with TestActorSystem {

  private val fakeRequest = FakeRequest("POST", routes.MessagesController.post.url)
  private val controller  = new MessagesController(stubControllerComponents())

  "POST /" should {
    "return 200" in {
      val result = controller.post()(fakeRequest)
      status(result) shouldBe Ok
    }
  }
}
