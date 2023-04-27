import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import play.sbt.routes.RoutesKeys

val appName = "transit-movements-eis-stub"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    majorVersion := 0,
    scalaVersion := "2.13.8",
    PlayKeys.playDefaultPort := 9476,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions += "-Wconf:src=routes/.*:s",
    RoutesKeys.routesImport ++= Seq(
      "models.Bindings._",
      "models.CustomsOffice"
    )
  )
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(CodeCoverageSettings.settings: _*)
  .settings(inThisBuild(buildSettings))

// Settings for the whole build
lazy val buildSettings = Def.settings(
  scalafmtOnCompile := true
)
