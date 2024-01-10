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

import org.apache.pekko.stream.FlowShape
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.SinkShape
import org.apache.pekko.stream.connectors.xml.ParseEvent
import org.apache.pekko.stream.connectors.xml.scaladsl.XmlParsing
import org.apache.pekko.stream.scaladsl.GraphDSL
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import cats.data.EitherT
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import com.google.inject.Singleton
import uk.gov.hmrc.transitmovementseisstub.models.LocalReferenceNumber
import uk.gov.hmrc.transitmovementseisstub.models.errors.ParserError
import uk.gov.hmrc.transitmovementseisstub.services.XmlParser.ParseResult

import scala.concurrent.Future

@ImplementedBy(classOf[LRNExtractorServiceImpl])
trait LRNExtractorService {
  def extractLRN(source: Source[ByteString, _]): EitherT[Future, ParserError, LocalReferenceNumber]

}

@Singleton
class LRNExtractorServiceImpl @Inject() (implicit mat: Materializer) extends LRNExtractorService {

  override def extractLRN(source: Source[ByteString, _]): EitherT[Future, ParserError, LocalReferenceNumber] =
    EitherT(source.runWith(lrnExtractor()))

  private val lrnSinkShape = Sink.head[ParseResult[LocalReferenceNumber]]

  private def lrnExtractor(): Sink[ByteString, Future[ParseResult[LocalReferenceNumber]]] =
    Sink.fromGraph(GraphDSL.createGraph(lrnSinkShape) {
      implicit builder => sink =>
        import GraphDSL.Implicits._

        val xmlParsing: FlowShape[ByteString, ParseEvent] = builder.add(XmlParsing.parser)
        val lrnSelector: FlowShape[ParseEvent, ParseResult[LocalReferenceNumber]] =
          builder.add(XmlParser.movementLRNExtractor("TraderChannelSubmission"))
        xmlParsing ~> lrnSelector ~> sink

        SinkShape(xmlParsing.in)
    })

}
