import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  private val catsVersion = "2.7.0"
  private val catsRetryVersion = "3.1.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % "8.4.0",
    "org.typelevel" %% "cats-core" % catsVersion,
    "com.github.cb372" %% "cats-retry" % catsRetryVersion,
    "org.apache.pekko" %% "pekko-slf4j" % "1.0.1",
    "org.apache.pekko" %% "pekko-connectors-xml" % "1.0.1"
  )

  val test = Seq(
    "uk.gov.hmrc"           %% "bootstrap-test-play-30"  % "8.4.0",
    "org.mockito"           %% "mockito-scala-scalatest" % "1.17.14",
    "org.scalatestplus"     %% "scalacheck-1-15"         % "3.2.11.0"
  ).map(_ % Test)
}
