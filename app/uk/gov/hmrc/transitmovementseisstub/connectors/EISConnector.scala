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

package uk.gov.hmrc.transitmovementseisstub.connectors

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.http.HeaderNames
import play.api.http.MimeTypes
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.transitmovementseisstub.config.EISInstanceConfig
import uk.gov.hmrc.transitmovementseisstub.connectors.errors.RoutingError

import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

trait EISConnector {
  def post(body: Source[ByteString, _])(implicit hc: HeaderCarrier): Future[Either[RoutingError, Unit]]
}

class EISConnectorImpl(
  val code: String,
  val eisInstanceConfig: EISInstanceConfig,
  httpClientV2: HttpClientV2,
  clock: Clock
)(implicit
  ec: ExecutionContext,
  val materializer: Materializer
) extends EISConnector
    with Logging {

  private val HTTP_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH).withZone(ZoneOffset.UTC)

  private def nowFormatted(): String =
    s"${HTTP_DATE_FORMATTER.format(OffsetDateTime.now(clock.withZone(ZoneOffset.UTC)))} UTC"

  override def post(body: Source[ByteString, _])(implicit hc: HeaderCarrier): Future[Either[RoutingError, Unit]] =
    httpClientV2
      .post(url"${eisInstanceConfig.url}")
      .setHeader(
        hc.headers(Seq("X-Conversation-Id", "X-Correlation-Id")): _*
      )
      .setHeader(
        HeaderNames.AUTHORIZATION -> hc.authorization.get.value,
        HeaderNames.CONTENT_TYPE  -> MimeTypes.XML,
        HeaderNames.ACCEPT        -> MimeTypes.XML,
        HeaderNames.DATE          -> nowFormatted()
      )
      .withBody(body)
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .map {
        case Right(_) =>
          Right(())
        case Left(error) =>
          Left(RoutingError(error.message, error.statusCode))
      }
      .recover {
        case NonFatal(e) =>
          val message = s"Request Error: Routing to $code failed to retrieve data with message ${e.getMessage}"
          Left(RoutingError(message, INTERNAL_SERVER_ERROR))
      }

}
