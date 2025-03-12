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

package uk.gov.hmrc.transitmovementseisstub.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import uk.gov.hmrc.transitmovementseisstub.base.StreamTestHelpers
import uk.gov.hmrc.transitmovementseisstub.base.TestActorSystem
import uk.gov.hmrc.transitmovementseisstub.models.LocalReferenceNumber
import uk.gov.hmrc.transitmovementseisstub.models.MessageSender
import uk.gov.hmrc.transitmovementseisstub.models.errors.ParserError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.NodeSeq

class LRNExtractorServiceSpec extends AnyFreeSpec with ScalaFutures with Matchers with TestActorSystem with StreamTestHelpers {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(6, Seconds))

  val validLRN: NodeSeq =
    <TraderChannelSubmission>
    <CC015C>
      <messageSender>abc</messageSender>
      <TransitOperation>
        <LRN>DUPLRN122233335</LRN>
      </TransitOperation>
    </CC015C>
    </TraderChannelSubmission>

  val invalidLRN: NodeSeq =
    <TraderChannelSubmission>
      <messageSender>abc</messageSender>
      <CC015C>
        <TransitOperation>
        </TransitOperation>
      </CC015C>
    </TraderChannelSubmission>

  val tooManyLRN: NodeSeq =
    <TraderChannelSubmission>
      <CC015C>
        <messageSender>abc</messageSender>
        <TransitOperation>
          <LRN>DUPLRN122233335</LRN>
          <LRN>DUPLRN122233335</LRN>
        </TransitOperation>
      </CC015C>
    </TraderChannelSubmission>

  "When handed an XML stream" - {
    val service = new LRNExtractorServiceImpl()

    "if it is valid and no LRN, return no element found error" in {
      val source = createStream(invalidLRN)

      val result = service.extractLRN(source)

      whenReady(result.value) {
        _ mustBe Left(ParserError.NoElementFound("LRN"))
      }
    }

    "if it is valid and has too many LRN, return Too many element found error" in {
      val source = createStream(tooManyLRN)

      val result = service.extractLRN(source)

      whenReady(result.value) {
        _ mustBe Left(ParserError.TooManyElementsFound("LRN"))
      }
    }

    "if it is valid and has LRN, return LocalReferenceNumber" in {
      val source = createStream(validLRN)

      val result = service.extractLRN(source)

      whenReady(result.value) {
        _ mustBe Right((LocalReferenceNumber("DUPLRN122233335"), MessageSender("abc")))
      }
    }
  }
}
