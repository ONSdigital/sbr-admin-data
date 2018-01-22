import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin.autoImport.{MergeStrategy, assembly, assemblyJarName, assemblyMergeStrategy}
import sbtassembly.PathList
import sbtbuildinfo.BuildInfoPlugin.autoImport.{buildInfoKeys, buildInfoOptions, buildInfoPackage}
import sbtbuildinfo.{BuildInfoKey, BuildInfoOption}
import sbtrelease.ReleasePlugin.autoImport.{releaseCommitMessage, releaseIgnoreUntrackedFiles, releaseTagComment}
import play.sbt.PlayImport.PlayKeys
import org.scalastyle.sbt.ScalastylePlugin.autoImport.{scalastyleFailOnError, scalastyleTarget}
import com.sksamuel.scapegoat.sbt.ScapegoatSbtPlugin.autoImport.{scapegoatConsoleOutput, scapegoatOutputPath}
import com.typesafe.sbt.SbtGit.git

import scoverage.ScoverageKeys.{coverageExcludedPackages, coverageFailOnMinimum, coverageMinimum}

/**
  * Common
  * ----------------
  * Author: haqa
  * Date: 02 November 2017 - 08:21
  * Copyright (c) 2017  Office for National Statistics
  */

object Common {


  /**
    * APP CONFIG
    */
  lazy val applicationConfig: SettingKey[Map[String, String]] = settingKey[Map[String, String]]("config values")
//  lazy val testScalaStyle = Scoped.AnyInitTask]("config values")


  /**
    * KEY-BINDING(S)
    */
  lazy val ITest: Configuration = config("it") extend Test


  /**
    * HARD VARS
    */
  lazy val Versions = new {
    val scala = "2.11.11"
    val appVersion = "0.1"
    val scapegoatVersion = "1.1.0"
  }

  lazy val Constant = new {
    val projectStage = "alpha"
    val team = "sbr"
    val local = "mac"
    val repoName = "admin-data"
  }


  /**
    * SETTINGS AND CONFIGURATION
    */
  lazy val Resolvers: Seq[MavenRepository] = Seq(
//    "Typesafe hbase.repository" at "http://repo.typesafe.com/typesafe/releases/",
    "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    Resolver.typesafeRepo("releases")
  )

  lazy val testSettings: Seq[Def.Setting[_]] = Seq(
    sourceDirectory in ITest := baseDirectory.value / "test/controllers/v1/it",
    resourceDirectory in ITest := baseDirectory.value / "test/resources",
    scalaSource in ITest := baseDirectory.value / "test/controllers/v1/it",
    // test setup
    parallelExecution in Test := false
  )

  lazy val noPublishSettings: Seq[Def.Setting[_]] = Seq(
    publish := {},
    publishLocal := {}
  )

  //@TODO - unify all map job in one + remove ._?. Tidy up.
  lazy val publishingSettings: Seq[Def.Setting[_]] = Seq(
    publishArtifact := applicationConfig.value("publishTrigger").toBoolean,
    publishMavenStyle := false,
    checksums in publish := Nil,
    publishArtifact in Test := false,
    publishArtifact in (Compile, packageBin) := false,
    publishArtifact in (Compile, packageSrc) := false,
    publishArtifact in (Compile, packageDoc) := false,
    //@TODO - switch to publishArtifact filter
//    publishArtifact in Compile := false,
//    publishArtifact in (Compile, assembly) := true,
    publishTo := {
      if (System.getProperty("os.name").toLowerCase.startsWith(Constant.local) )
        Some(Resolver.file("file", new File(s"${System.getProperty("user.home").toLowerCase}/Desktop/")))
      else
        Some("Artifactory Realm" at applicationConfig.value("artifactoryAddress"))
    },
    artifact in (Compile, assembly) ~= { art =>
      art.copy(`type` = "jar", `classifier` = Some("assembly"))
    },
    //@TODO - specify the scope in Compile
    //artifactName in (Compile, assembly)
    artifactName := { (sv: ScalaVersion, module: ModuleID, artefact: Artifact) =>
      module.organization + "_" + artefact.name + "-" + artefact.classifier.getOrElse("package") + "-" + module.revision + "." + artefact.extension
    },
    credentials += Credentials("Artifactory Realm", applicationConfig.value("artifactoryHost"),
      applicationConfig.value("artifactoryUser"), applicationConfig.value("artifactoryPassword")),
    releaseTagComment := s"Releasing $name ${(version in ThisBuild).value}",
    releaseCommitMessage := s"Setting Release tag to ${(version in ThisBuild).value}",
    // no commit - ignore zip and other package files
    releaseIgnoreUntrackedFiles := true
  )

  lazy val buildInfoConfig: Seq[Def.Setting[_]] = Seq(
    buildInfoPackage := "controllers",
    // gives us last compile time and tagging info
    buildInfoKeys := Seq[BuildInfoKey](
      organizationName,
      moduleName,
      name,
      description,
      developers,
      version,
      scalaVersion,
      sbtVersion,
      startYear,
      homepage,
      BuildInfoKey.action("gitVersion") {
        git.formattedShaVersion.?.value.getOrElse(Some("Unknown")).getOrElse("Unknown") +"@"+ git.formattedDateVersion.?.value.getOrElse("")
      },
      BuildInfoKey.action("codeLicenses"){ licenses.value },
      BuildInfoKey.action("projectTeam"){ Constant.team },
      BuildInfoKey.action("projectStage"){ Constant.projectStage },
      BuildInfoKey.action("repositoryAddress"){ Some(scmInfo.value.get.browseUrl).getOrElse("REPO_ADDRESS_NOT_FOUND")}
    ),
    buildInfoOptions += BuildInfoOption.ToMap,
    buildInfoOptions += BuildInfoOption.ToJson,
    buildInfoOptions += BuildInfoOption.BuildTime
  )

  lazy val commonSettings: Seq[Def.Setting[_]] = Seq (
    organizationName := "ons",
    organization := "uk.gov.ons",
    developers := List(Developer("Adrian Harris (Tech Lead)", "SBR", "ons-sbr-team@ons.gov.uk", new java.net.URL(s"https:///v1/home"))),
    version := (version in ThisBuild).value,
    licenses := Seq("MIT-License" -> url("https://github.com/ONSdigital/sbr-control-api/blob/master/LICENSE")),
    startYear := Some(2017),
    homepage := Some(url("https://SBR-UI-HOMEPAGE.gov.uk")),
    scalaVersion := Versions.scala,
    name := s"${organizationName.value}-${Constant.team}-${Constant.repoName}",
    scalacOptions in ThisBuild ++= Seq(
      "-language:experimental.macros",
      "-target:jvm-1.8",
      "-encoding", "UTF-8",
      "-language:reflectiveCalls",
      "-language:experimental.macros",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:postfixOps",
      "-deprecation", // warning and location for usages of deprecated APIs
      "-feature", // warning and location for usages of features that should be imported explicitly
      "-unchecked", // additional warnings where generated code depends on assumptions
      "-Xlint", // recommended additional warnings
      "-Xcheckinit", // runtime error when a val is not initialized due to trait hierarchies (instead of NPE somewhere else)
      "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
      //"-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver
      "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
      "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures
      "-Ywarn-dead-code", // Warn when dead code is identified
      "-Ywarn-unused", // Warn when local and private vals, vars, defs, and types are unused
      "-Ywarn-unused-import", //  Warn when imports are unused (don't want IntelliJ to do it automatically)
      "-Ywarn-numeric-widen" // Warn when numerics are widened
    ),
    logLevel := Level.Warn,
    resolvers ++= Resolvers,
    coverageExcludedPackages := ".*Routes.*;.*ReverseRoutes.*;.*javascript.*;controllers\\..*Reverse.*",
    coverageMinimum := 80,
    coverageFailOnMinimum := true,
    scalastyleTarget := (target.value / "code-quality/style/scalastyle-result.xml"),
    scalastyleFailOnError := true,
    scapegoatOutputPath := "target/code-quality/style",
    scapegoatConsoleOutput := false
  )

  lazy val assemblySettings: Seq[Def.Setting[_]] = Seq(
    assemblyJarName in assembly := s"${name.value}-assembly-${version.value}.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("org", "apache", xs@_*)                             => MergeStrategy.last
      case PathList("META-INF", "io.netty.versions.properties", xs@_ *) => MergeStrategy.last
      case "application.conf"                                           => MergeStrategy.first
      case "logback.xml"                                                => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )

}
