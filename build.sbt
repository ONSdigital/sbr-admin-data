//import com.github.sbt.jacoco.JacocoPlugin.jacoco

name := "sbr-admin-data"

lazy val commonSettings = Seq(
  organization := "uk.gov.ons",
  version := "1.0",
  scalaVersion := "2.11.8",
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
)

lazy val `sbr-admin-data` = (project in file("."))
  .settings(commonSettings)
  .enablePlugins(PlayScala)
  .dependsOn(model)
  .dependsOn(`repository-hbase`)
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

libraryDependencies ++= Seq( cache , ws )

// Metrics
libraryDependencies += "io.dropwizard.metrics" % "metrics-core" % "3.2.5"
dependencyOverrides += "com.google.guava" % "guava" % "14.0.1"

// Mockito
libraryDependencies += "org.mockito" % "mockito-core" % "2.10.0" % "test"

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

