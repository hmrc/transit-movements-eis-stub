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

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.connectors.xml.ParseEvent
import org.apache.pekko.stream.connectors.xml.scaladsl.XmlParsing
import org.apache.pekko.stream.scaladsl.Flow
import uk.gov.hmrc.transitmovementseisstub.models.LocalReferenceNumber

object XmlParser extends XmlParsingServiceHelpers {

  def movementLRNExtractor(
    rootNode: String
  ): Flow[ParseEvent, ParseResult[LocalReferenceNumber], NotUsed] =
    XmlParsing
      .subtree(rootNode :: "CC015C" :: "TransitOperation" :: "LRN" :: Nil)
      .collect {
        case element if element.getTextContent.nonEmpty => LocalReferenceNumber(element.getTextContent)
      }
      .single("LRN")

}
