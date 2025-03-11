import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  private val catsVersion = "2.7.0"
  private val catsRetryVersion = "3.1.3"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % "9.3.0",
    "org.typelevel" %% "cats-core" % catsVersion,
    "com.github.cb372" %% "cats-retry" % catsRetryVersion,
    "org.apache.pekko" %% "pekko-slf4j" % "1.1.3",
    "org.apache.pekko" %% "pekko-connectors-xml" % "1.1.0",
    "org.apache.pekko" %% "pekko-protobuf-v3" % "1.1.3",
    "org.apache.pekko" %% "pekko-serialization-jackson" % "1.1.3",
    "org.apache.pekko" %% "pekko-stream" % "1.1.3",
    "org.apache.pekko" %% "pekko-actor-typed" % "1.1.3",
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"           %% "bootstrap-test-play-30"  % "9.3.0",
    "org.scalatestplus" %% "mockito-5-12"            % "3.2.19.0",
    "org.scalacheck"    %% "scalacheck"              % "1.18.1",
    "org.scalatestplus" %% "scalacheck-1-18"         % "3.2.19.0",
    "org.apache.pekko" %% "pekko-serialization-jackson" % "1.1.3",
    "org.apache.pekko" %% "pekko-actor-typed" % "1.1.3"
  ).map(_ % Test)
}
