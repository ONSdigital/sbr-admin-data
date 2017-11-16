
//import com.github.sbt.jacoco.JacocoPlugin.jacoco

name := "sbr-admin-data"

lazy val commonSettings = Seq(
  organization := "uk.gov.ons",
  version := "1.0",
  scalaVersion := "2.11.8",
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
)

// This is so we can use Options in the routes file
routesImport += "extensions.Binders._"

lazy val `sbr-admin-data` = (project in file("."))
  .settings(commonSettings)
  .settings(
    buildInfoPackage := "controllers",
    buildInfoKeys := Seq[BuildInfoKey](
      organization,
      name,
      version,
      scalaVersion,
      sbtVersion
    ))
  .enablePlugins(PlayScala, BuildInfoPlugin)
  .dependsOn(model, `repository-hbase`)
  .aggregate(model, `repository-hbase`)

lazy val model = project
  .settings(commonSettings)

lazy val `repository-hbase` = project
  .settings(commonSettings)
  .dependsOn(model)

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.4"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"
logBuffered in Test := false
libraryDependencies += "junit" % "junit" % "4.12" % Test
crossPaths := false
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test
testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a"))
/*lazy val jacocoSettings = Seq(
  parallelExecution := true,
  jacocoExcludes := Seq("views*", "*Routes*", "controllers*routes*", "controllers*Reverse*", "controllers*javascript*", "controller*ref*")
)*/

libraryDependencies ++= Seq( cache , ws, filters )

// Metrics
libraryDependencies += "io.dropwizard.metrics" % "metrics-core" % "3.2.5"
dependencyOverrides += "com.google.guava" % "guava" % "14.0.1"

// Mockito
libraryDependencies += "org.mockito" % "mockito-core" % "2.10.0" % "test"

// Scala Logging
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"

// Scalaz, for validation
libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.2.16"

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

