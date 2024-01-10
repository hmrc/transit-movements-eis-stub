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

package uk.gov.hmrc.transitmovements.services

import org.apache.pekko.stream.scaladsl.Sink
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.transitmovementseisstub.base.StreamTestHelpers
import uk.gov.hmrc.transitmovementseisstub.base.TestActorSystem
import uk.gov.hmrc.transitmovementseisstub.models.LocalReferenceNumber
import uk.gov.hmrc.transitmovementseisstub.models.errors.ParserError
import uk.gov.hmrc.transitmovementseisstub.services.XmlParser

import scala.xml.NodeSeq

class XmlParserSpec extends AnyFreeSpec with TestActorSystem with Matchers with StreamTestHelpers with ScalaFutures with ScalaCheckPropertyChecks {

  "LRN parser" - {

    val withEntry: NodeSeq =
      <TraderChannelSubmission>
        <ncts:CC015C PhaseID='NCTS5.0' xmlns:ncts='http://ncts.dgtaxud.ec'>
          <TransitOperation>
            <LRN>DUPLRN99999999</LRN>
          </TransitOperation>
        </ncts:CC015C>
      </TraderChannelSubmission>

    val withNoEntry: NodeSeq =
      <CC015C>
      </CC015C>

    val withTwoEntries: NodeSeq =
      <TraderChannelSubmission>
        <ncts:CC015C PhaseID='NCTS5.0' xmlns:ncts='http://ncts.dgtaxud.ec'>
          <TransitOperation>
            <LRN>DUPLRN99999999</LRN>
            <LRN>DUPLRN89663663</LRN>
          </TransitOperation>
        </ncts:CC015C>
      </TraderChannelSubmission>

    "when provided with a valid entry" in {
      val stream       = createParsingEventStream(withEntry)
      val parsedResult = stream.via(XmlParser.movementLRNExtractor("TraderChannelSubmission")).runWith(Sink.head)

      whenReady(parsedResult) {
        _ mustBe Right(LocalReferenceNumber("DUPLRN99999999"))
      }
    }

    "when provided with no entry" in {
      val stream       = createParsingEventStream(withNoEntry)
      val parsedResult = stream.via(XmlParser.movementLRNExtractor("TraderChannelSubmission")).runWith(Sink.head)

      whenReady(parsedResult) {
        _ mustBe Left(ParserError.NoElementFound("LRN"))
      }
    }

    "when provided with two entries" in {
      val stream       = createParsingEventStream(withTwoEntries)
      val parsedResult = stream.via(XmlParser.movementLRNExtractor("TraderChannelSubmission")).runWith(Sink.head)

      whenReady(parsedResult) {
        _ mustBe Left(ParserError.TooManyElementsFound("LRN"))
      }
    }

  }
}
