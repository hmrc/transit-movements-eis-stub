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

package uk.gov.hmrc.transitmovementseisstub.controllers

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.EitherT
import play.api.Logging
import play.api.http.HeaderNames
import play.api.http.MimeTypes
import play.api.http.Writeable
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.{HeaderNames => HMRCHeaderNames}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.transitmovementseisstub.config.AppConfig
import uk.gov.hmrc.transitmovementseisstub.connectors.EISConnectorProvider
import uk.gov.hmrc.transitmovementseisstub.controllers.stream.StreamingParsers
import uk.gov.hmrc.transitmovementseisstub.models.CustomsOffice
import uk.gov.hmrc.transitmovementseisstub.models.LocalReferenceNumber
import uk.gov.hmrc.transitmovementseisstub.models.MessageSender
import uk.gov.hmrc.transitmovementseisstub.models.errors.HeaderError
import uk.gov.hmrc.transitmovementseisstub.models.errors.StubError
import uk.gov.hmrc.transitmovementseisstub.models.errors.XMLError
import uk.gov.hmrc.transitmovementseisstub.services.LRNExtractorService

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import scala.concurrent.Future
import scala.util.Success
import scala.util.Try

class MessagesController @Inject() (
  appConfig: AppConfig,
  cc: ControllerComponents,
  eisConnectorProvider: EISConnectorProvider,
  lrnExtractorService: LRNExtractorService
)(implicit
  val materializer: Materializer
) extends BackendController(cc)
    with StreamingParsers
    with Logging {

  private val DATE_FORMAT_REGEX    = "^(.+) UTC$".r
  private val HTTP_DATE_FORMATTER  = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH).withZone(ZoneOffset.UTC)
  private val UUID_PATTERN         = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$".r
  private val BEARER_TOKEN_PATTERN = "^Bearer (\\S+)$".r
  private lazy val lrnRegexPattern = """^DUPLRN\d*$""".r

  def routeToEIScheck(client: String, customsOffice: CustomsOffice): Boolean =
    (appConfig.clientAllowList.contains(client) && customsOffice.proxyEnabled(appConfig)) || appConfig.internalAllowList.contains(client)

  def post(customsOffice: CustomsOffice): Action[Source[ByteString, _]] = Action.async(streamFromMemory) {
    implicit request: Request[Source[ByteString, _]] =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
      hc.headers(Seq("X-Client-Id")) match {
        case Seq(clientHeaderAndValue) if routeToEIScheck(clientHeaderAndValue._2, customsOffice) =>
          routeToEIS(customsOffice)
        case _ =>
          routeToStub
      }
  }

  def postToStub: Action[Source[ByteString, _]] = Action.async(streamFromMemory) {
    implicit request: Request[Source[ByteString, _]] =>
      routeToStub
  }

  private def routeToStub(implicit request: Request[Source[ByteString, _]]) =
    (for {
      _ <- validateHeader("X-Correlation-Id", isUuid)
      _ <- validateHeader("X-Conversation-Id", isUuid)
      _ <- validateHeader(HeaderNames.CONTENT_TYPE, isXml)
      _ <- validateHeader(HeaderNames.ACCEPT, isXml)
      _ <- validateHeader(HeaderNames.AUTHORIZATION, isBearerToken)
      _ <- validateHeader(HeaderNames.DATE, isDate)
      _ <- validateLRN(request.body)
    } yield ()).fold[Result](
      {
        error =>
          logger.error(s"""Request failed with the following error:
                       |${error.errorMessage}
                       |
                       |Request ID: ${request.headers.get(HMRCHeaderNames.xRequestId).getOrElse("unknown")}
                       |""".stripMargin)
          error match {
            case _: HeaderError => Status(FORBIDDEN) // EIS does not send a message ordinarily.
            case x: XMLError    => Status(FORBIDDEN)(x.errorMessage)(Writeable(ByteString.apply, Some(MimeTypes.XML)))
          }
      },
      _ => Ok
    )

  private def routeToEIS(customsOffice: CustomsOffice)(implicit request: Request[Source[ByteString, _]], hc: HeaderCarrier) = {
    val connector = (customsOffice: @unchecked) match {
      case CustomsOffice.Gb => eisConnectorProvider.gb
      case CustomsOffice.Xi => eisConnectorProvider.xi
    }

    connector.post(request.body)(hc).map {
      case Right(_) => Ok
      case Left(error) =>
        Status(error.statusCode)(error.message)
    }
  }

  private def validateHeader(header: String, validation: String => Option[String])(implicit
    request: Request[Source[ByteString, _]]
  ): EitherT[Future, StubError, Unit] =
    EitherT.fromEither[Future] {
      request.headers.get(header) match {
        case Some(value) =>
          validation(value)
            .map {
              result =>
                request.body.runWith(Sink.ignore)
                HeaderError(s"Error in header $header: $result")
            }
            .toLeft(())
        case None =>
          request.body.runWith(Sink.ignore)
          Left(HeaderError(s"Required header is missing: $header"))
      }
    }

  private def isUuid(value: String): Option[String] = if (UUID_PATTERN.matches(value)) None else Some(s"$value is not a UUID")

  private def isXml(value: String): Option[String] = if (value == MimeTypes.XML) None else Some(s"Expected application/xml, got $value")

  private def isBearerToken(value: String): Option[String] =
    value match {
      case BEARER_TOKEN_PATTERN(v) =>
        if (appConfig.enforceAuthToken && appConfig.authToken != v) Some("Bearer token does not match expected token")
        else None
      case _ =>
        Some("Bearer token not in expected format")
    }

  private def isDate(value: String): Option[String] =
    DATE_FORMAT_REGEX
      .findFirstMatchIn(value)
      .map(_.group(1))
      .map(
        x => Try(HTTP_DATE_FORMATTER.parse(x))
      ) match {
      case Some(Success(_)) => None
      case _                => Some(s"Expected date in RFC 7231 format, instead got $value")
    }

  private def validateLRN(requestBody: Source[ByteString, _]): EitherT[Future, StubError, Unit] = EitherT {
    (for {
      lrn <- lrnExtractorService.extractLRN(requestBody)
    } yield lrn).fold(
      _ => Right(()),
      lrnAndMessageSender =>
        if (lrnRegexPattern.matches(lrnAndMessageSender._1.value)) {
          createErrorXml(lrnAndMessageSender._1, lrnAndMessageSender._2)
        } else {
          Right(())
        }
    )
  }

  private def createErrorXml(lrn: LocalReferenceNumber, messageSender: MessageSender): Either[StubError, Unit] =
    Left(XMLError(s"""<?xml version="1.0" encoding="UTF-8"?>
          |<lrnDuplicationErrorDetail xmlns:eis="http://www.hmrc.gsi.gov.uk/eis">
          |    <timestamp>Fri, 03 Jul 2020 11:20:25 UTC</timestamp>
          |    <correlationId>00000000-0000-0000-0000-000000000000</correlationId>
          |    <errorCode>400</errorCode>
          |    <errorMessage>The supplied LRN: ${lrn.value} has already been used by submitter: ${messageSender.value}</errorMessage>
          |    <source>ERMIS</source>
          |    <sourceFaultDetail>
          |        <submitter>${messageSender.value}</submitter>
          |        <lrn>${lrn.value}</lrn>
          |    </sourceFaultDetail>
          |</lrnDuplicationErrorDetail>
          |""".stripMargin))

}
