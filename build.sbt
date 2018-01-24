import com.typesafe.config.ConfigFactory

/**
  * APP CONFIG
  */
Common.applicationConfig := {
  val conf = ConfigFactory.parseFile((resourceDirectory in Compile).value / "application.conf").resolve()
  val artifactoryConf = conf.getConfig("artifactory")
  Map (
    "publishTrigger" -> artifactoryConf.getBoolean("publish-init").toString,
    "artifactoryAddress" -> artifactoryConf.getString("publish-hbase.hbase.repository"),
    "artifactoryHost" -> artifactoryConf.getString("host"),
    "artifactoryUser" -> artifactoryConf.getString("user"),
    "artifactoryPassword" -> artifactoryConf.getString("password")
  )
}

/**
  * SETTINGS AND CONFIGURATION
  */

// TODO - CHANGE covergMinimum back
//coverageMinimum := 55
coverageMinimum := 50

lazy val initExec: Seq[Def.Setting[_]] = Seq(
  crossPaths := false,
  parallelExecution := false,
  // Java heap memory memory allocation - lots of deps
  javaOptions += "-Xmx2G"
)

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a")),
  fork in test := true,
  logBuffered in Test := false
)

//@TODO - merge dup deps
lazy val devDeps = Seq(
  cache,
  ws,
  filters,
  "org.scalactic"              %%  "scalactic"       %   "3.0.4",
  "org.scalatest"              %%  "scalatest"       %   "3.0.4"     %   "test",
  "org.webjars"                %%  "webjars-play"    %   "2.5.0-3",
  "io.swagger"                 %%  "swagger-play2"   %   "1.5.3",
  "org.webjars"                %   "swagger-ui"      %   "2.2.10-1",
  // Metrics
  "io.dropwizard.metrics"      %   "metrics-core"    %   "3.2.5",
  // Mockito
  "org.mockito"                %   "mockito-core"    %   "2.10.0"    %   "test",
  "com.novocode"               %   "junit-interface" %   "0.11"      %   Test,
  "junit"                      %   "junit"           %   "4.12"      %   Test,
  // Controller
  "io.swagger"                 %%  "swagger-play2"   %   "1.5.1",
  "com.typesafe.scala-logging" %%  "scala-logging"   %   "3.7.2",
  "ch.qos.logback"             %   "logback-classic" %   "1.2.3",
  "org.scalatestplus.play"     %%  "scalatestplus-play" % "2.0.0" % "test"
)

// Run tests with full stack traces
testOptions in Test += Tests.Argument("-oG")

javaOptions in Test += "-Dconfig.file=test/resources/application.test.conf"

/**
  * PROJECT DEF
  */
lazy val `sbr-admin-data` = (project in file("."))
  .enablePlugins(BuildInfoPlugin, GitVersioning, GitBranchPrompt, PlayScala)
  .enablePlugins(AssemblyPlugin)
  .configs(Common.ITest)
  .settings(inConfig(Common.ITest)(Defaults.testSettings) : _*)
  .settings(Common.commonSettings: _*)
  .settings(Common.testSettings:_*)
  .settings(testSettings:_*)
  //.settings(Common.noPublishSettings:_*)
  .settings(Common.publishingSettings:_*)
  .settings(Common.buildInfoConfig:_*)
  .settings(initExec:_*)
  .settings(Common.assemblySettings:_*)
  .settings(
    routesImport += "extensions.Binders._",
      //moduleName := "sbr-admin-data",
    description := "<description>",
    libraryDependencies ++= devDeps,
    // di router -> swagger
    routesGenerator := InjectedRoutesGenerator,
    dependencyOverrides += "com.google.guava" % "guava" % "14.0.1",
    unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")
  )
  .dependsOn(model)
  .dependsOn(`repository-hbase`)
  .aggregate(model, `repository-hbase`)

lazy val model = project
  .disablePlugins(AssemblyPlugin)
  .settings(Common.commonSettings: _*)


lazy val `repository-hbase` = project
  .disablePlugins(AssemblyPlugin)
  .settings(Common.commonSettings: _*)
//  .settings(Common.assemblySettings:_*)
  .dependsOn(model)
