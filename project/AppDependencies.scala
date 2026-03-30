import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  private val catsVersion = "2.13.0"
  private val bootstrapVersion = "10.7.0"
  private val catsRetryVersion = "4.0.0"
  private val pekkoVersion = "1.4.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapVersion,
    "org.typelevel" %% "cats-core" % catsVersion,
    "com.github.cb372" %% "cats-retry" % catsRetryVersion,
    "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion,
    "org.apache.pekko" %% "pekko-connectors-xml" % "1.3.0",
    "org.apache.pekko" %% "pekko-protobuf-v3" % pekkoVersion,
    "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
    "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
    "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"           %% "bootstrap-test-play-30"  % bootstrapVersion,
    "org.scalatestplus" %% "mockito-5-12"            % "3.2.19.0",
    "org.scalacheck"    %% "scalacheck"              % "1.19.0",
    "org.scalatestplus" %% "scalacheck-1-18"         % "3.2.19.0",
    "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
    "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion
  ).map(_ % Test)
}
