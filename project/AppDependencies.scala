import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % "5.24.0"
  )

  val test = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-28" % "5.24.0",
    "com.vladsch.flexmark" % "flexmark-all"           % "0.36.8"
  ).map(_ % "test, it")
}
