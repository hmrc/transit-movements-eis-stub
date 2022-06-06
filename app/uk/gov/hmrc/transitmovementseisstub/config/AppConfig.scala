package uk.gov.hmrc.transitmovementseisstub.config

import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration

@Singleton
class AppConfig @Inject() (config: Configuration) {

  lazy val appName: String = config.get[String]("appName")
}
