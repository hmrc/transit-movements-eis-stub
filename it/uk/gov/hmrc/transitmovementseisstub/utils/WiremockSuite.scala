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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.kenshoo.play.metrics.Metrics
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import org.scalatestplus.play.guice.GuiceFakeApplicationFactory
import play.api.Application
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.inject.{Injector, bind}

import java.time.Clock

trait WiremockSuite extends BeforeAndAfterAll with BeforeAndAfterEach {
  this: Suite =>
  protected val wiremockPort = 11111

  protected val wiremockConfig =
    wireMockConfig().dynamicPort().port(wiremockPort).notifier(new ConsoleNotifier(false))

  protected val server: WireMockServer = new WireMockServer(wiremockConfig)

  override def beforeAll(): Unit = {
    server.start()
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    server.resetAll()
    super.beforeEach()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    server.stop()
  }
}

trait WiremockSuiteWithGuice extends WiremockSuite {
  this: Suite with GuiceFakeApplicationFactory =>

  override def fakeApplication(): Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.eis.gb.port"          -> server.port().toString,
        "microservice.services.eis.xi.port"          -> server.port().toString,
        "microservice.services.ncts-monitoring.port" -> server.port().toString
      )
      .overrides(bindings: _*)

  protected lazy val injector: Injector = fakeApplication.injector

  protected def bindings: Seq[GuiceableModule] = Seq(
    bind[Metrics].toInstance(new TestMetrics),
    bind[Clock].toInstance(Clock.systemUTC())
  )

  override def beforeAll(): Unit = {
    server.start()
    fakeApplication
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    server.resetAll()
    fakeApplication
    super.beforeEach()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    server.stop()
  }

}
