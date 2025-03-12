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

package uk.gov.hmrc.transitmovementseisstub.utils

import izumi.reflect.Tag
import play.api.libs.ws.BodyWritable
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.client.RequestBuilder
import uk.gov.hmrc.http.client.StreamHttpReads

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class FakeRequestBuilder extends RequestBuilder {
  override def execute[A: HttpReads](implicit ec: ExecutionContext): Future[A] = Future.failed(new RuntimeException)

  override def setHeader(header: (String, String)*): RequestBuilder = this

  override def withBody[B: BodyWritable: Tag](body: B)(implicit ec: ExecutionContext): RequestBuilder = this

  override def transform(transform: WSRequest => WSRequest): RequestBuilder = this

  override def stream[A: StreamHttpReads](implicit ec: ExecutionContext): Future[A] = Future.failed(new RuntimeException)

  override def withProxy: RequestBuilder = this
}
