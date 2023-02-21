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

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Logging
import play.api.http.HeaderNames
import play.api.http.Status.SERVICE_UNAVAILABLE
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderNames => HMRCHeaderNames}
import uk.gov.hmrc.transitmovementseisstub.config.EISInstanceConfig
import uk.gov.hmrc.transitmovementseisstub.models.EISResponse
import uk.gov.hmrc.transitmovementseisstub.utils.RouterHeaderNames

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

trait EISConnector {

  def post(body: Source[ByteString, _])(implicit headerCarrier: HeaderCarrier): Future[EISResponse]

}

class EISConnectorImpl(
  val code: String,
  val eisInstanceConfig: EISInstanceConfig,
  headerCarrierConfig: HeaderCarrier.Config,
  httpClientV2: HttpClientV2,
)(implicit
  ec: ExecutionContext,
  val materializer: Materializer
) extends EISConnector
    with Logging {

  private lazy val eisError = EISResponse(
    SERVICE_UNAVAILABLE,
    Json.stringify(Json.obj("code" -> "SERVICE_UNAVAILABLE", "message" -> "Failed to connect to EIS"))
  )

  private def getHeader(header: String, url: String)(implicit hc: HeaderCarrier): String =
    hc
      .headersForUrl(headerCarrierConfig)(url)
      .find {
        case (name, _) => name.toLowerCase == header.toLowerCase
      }
      .map {
        case (_, value) => value
      }
      .getOrElse("undefined")

  override def post(body: Source[ByteString, _])(implicit headerCarrier: HeaderCarrier): Future[EISResponse] = {

    val requestId = getHeader(HMRCHeaderNames.xRequestId, eisInstanceConfig.url)(headerCarrier)
    lazy val logMessage =
      s"""|Stub is operating in PROXY mode for client ID ${getHeader("x-client-id", eisInstanceConfig.url)(headerCarrier)}.
          |Posting NCTS message, routing to $code
          |${RouterHeaderNames.CORRELATION_ID}: ${getHeader(RouterHeaderNames.CORRELATION_ID, eisInstanceConfig.url)(headerCarrier)}
          |${HMRCHeaderNames.xRequestId}: $requestId
          |${RouterHeaderNames.MESSAGE_TYPE}: ${getHeader(RouterHeaderNames.MESSAGE_TYPE, eisInstanceConfig.url)(headerCarrier)}
          |${RouterHeaderNames.CONVERSATION_ID}: ${getHeader(RouterHeaderNames.CONVERSATION_ID, eisInstanceConfig.url)(headerCarrier)}
          |${HeaderNames.ACCEPT}: ${getHeader(HeaderNames.ACCEPT, eisInstanceConfig.url)(headerCarrier)}
          |${HeaderNames.CONTENT_TYPE}: ${getHeader(HeaderNames.CONTENT_TYPE, eisInstanceConfig.url)(headerCarrier)}
          |${RouterHeaderNames.CUSTOM_PROCESS_HOST}: ${getHeader(RouterHeaderNames.CUSTOM_PROCESS_HOST, eisInstanceConfig.url)(headerCarrier)}
          |""".stripMargin

    httpClientV2
      .post(url"${eisInstanceConfig.url}")
      .withBody(body)
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .map {
        case Right(result) =>
          logger.info(logMessage + s"Response status: ${result.status}")
          EISResponse(result.status, result.body)
        case Left(error) =>
          logger.warn(logMessage + s"Response status: ${error.statusCode}")
          EISResponse(error.statusCode, error.message)
      }
      .recover {
        case NonFatal(e) =>
          val message = logMessage + s"Request Error: Routing to $code failed to retrieve data with message ${e.getMessage}"
          logger.error(message)
          eisError
      }
  }

}
