import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % "7.12.0"
  )

  val test = Seq(
    "uk.gov.hmrc"           %% "bootstrap-test-play-28"  % "7.12.0",
    "org.mockito"           %% "mockito-scala-scalatest" % "1.17.12",
    "org.mockito"            % "mockito-core"            % "3.12.4",
    "org.scalatestplus"     %% "scalacheck-1-15"         % "3.2.11.0",
    "com.vladsch.flexmark"   % "flexmark-all"            % "0.62.2",
    "com.github.tomakehurst" % "wiremock-standalone"     % "2.27.2"
  ).map(_ % "test, it")
}
