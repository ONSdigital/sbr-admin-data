import sbt.ExclusionRule


/**
  * VALUES
  */
lazy val Versions = new {
//  val hbaseVersion = "1.3.1"
//  val hadoopVersion = "2.5.1"
  //  val sparkVersion = "2.2.0"
  val hbaseVersion = "1.2.0-cdh5.10.1"
  val hadoopVersion = "2.6.0-cdh5.10.1"
  val clouderaHBase = "1.2.0-cdh5.10.1"
  val clouderaHadoop = "2.6.0-cdh5.10.1"
}

lazy val Constants = new {
  //orgs
  val apacheHBase = "org.apache.hbase"
  val apacheHadoop = "org.apache.hadoop"
}


/**
  * DEPENDENCIES LISTINGS
  */
lazy val hadoopDeps: Seq[ModuleID] = Seq(
  // HBase
  Constants.apacheHBase   % "hbase-common"                      % Versions.clouderaHBase,
  Constants.apacheHBase   % "hbase-common"                      % Versions.clouderaHBase   classifier "tests",
  Constants.apacheHBase   % "hbase-client"                      % Versions.clouderaHBase   exclude ("org.slf4j", "slf4j-api"),
  Constants.apacheHBase   % "hbase-hadoop-compat"               % Versions.clouderaHBase,
  Constants.apacheHBase   % "hbase-hadoop-compat"               % Versions.clouderaHBase   classifier "tests",
  Constants.apacheHBase   % "hbase-hadoop2-compat"              % Versions.clouderaHBase,
  Constants.apacheHBase   % "hbase-hadoop2-compat"              % Versions.clouderaHBase   classifier "tests",
  Constants.apacheHBase   % "hbase-server"                      % Versions.clouderaHBase,
  Constants.apacheHBase   % "hbase-server"                      % Versions.clouderaHBase   classifier "tests",

  // Hadoop
  Constants.apacheHadoop  % "hadoop-common"                     % Versions.clouderaHadoop,
  Constants.apacheHadoop  % "hadoop-common"                     % Versions.clouderaHadoop  classifier "tests",
  Constants.apacheHadoop  % "hadoop-hdfs"                       % Versions.clouderaHadoop,
  Constants.apacheHadoop  % "hadoop-hdfs"                       % Versions.clouderaHadoop  classifier "tests",
  Constants.apacheHadoop  % "hadoop-mapreduce-client-core"      % Versions.clouderaHadoop,
  Constants.apacheHadoop  % "hadoop-mapreduce-client-jobclient" % Versions.clouderaHadoop
).map(_.excludeAll ( ExclusionRule("log4j", "log4j"), ExclusionRule ("org.slf4j", "slf4j-log4j12")))


lazy val DevDeps: Seq[ModuleID] = Seq(
  // scala-date
  "com.github.nscala-time"  %%  "nscala-time"                   % "2.16.0",

  // Akka
  "com.typesafe.akka"       %% "akka-actor"                     % "2.5.6",
  "com.typesafe.akka"       %% "akka-testkit"                   % "2.5.6"                  % Test,

  // CSV Parser
  "net.sf.opencsv"          % "opencsv"                         % "2.3",

  // Failsafe
  "net.jodah"               % "failsafe"                        % "1.0.4",

  // Mockito
  "org.mockito"             % "mockito-core"                    % "2.10.0"                 % "test",

  // logging
  "org.slf4j"               % "slf4j-api"                       % "1.7.25",
  "org.slf4j"               % "log4j-over-slf4j"                % "1.7.25",
  "ch.qos.logback"          % "logback-classic"                 % "1.2.3"                  % "test",

  // testing
  "org.scalactic"           %%  "scalactic"                     % "3.0.4",
  "org.scalatest"           %%  "scalatest"                     % "3.0.4"                  % "test",

  //junit
  "com.novocode"            % "junit-interface"                 % "0.11"                   % Test,
  "junit"                   % "junit"                           % "4.12"                   % Test
) ++ hadoopDeps.map(_ % "provided")

// Metrics
dependencyOverrides += "com.google.guava"        % "guava"                           % "14.0.1"

lazy val exTransiviveDeps: Seq[ExclusionRule] = Seq(
  ExclusionRule("commons-logging", "commons-logging"),
  ExclusionRule("log4j", "log4j"),
  ExclusionRule ("org.slf4j", "slf4j-log4j12")
)

/**
  * PROJECT DEF
  */
moduleName := "sbr-admin-data-repository-hbase"
description := "<description>"
libraryDependencies ++=  DevDeps
//excludeDependencies ++= exTransiviveDeps
resolvers += "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"
mainClass in (Compile, packageBin) := Some("hbase.load.BulkLoader")


crossPaths := false
testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a"))
logBuffered in Test := false
