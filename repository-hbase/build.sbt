import sbt.ExclusionRule


lazy val Versions = new {
  val hbaseVersion = "1.3.1"
  val hadoopVersion = "2.5.1"
  val sparkVersion = "2.2.0"
}

lazy val Constants = new {
  //orgs
  val apacheHBase = "org.apache.hbase"
  val apacheHadoop = "org.apache.hadoop"
}

lazy val hadoopDeps: Seq[ModuleID] = Seq(
  // HBase
  Constants.apacheHBase   % "hbase-common"                      % Versions.hbaseVersion,
  Constants.apacheHBase   % "hbase-common"                      % Versions.hbaseVersion   classifier "tests",
  Constants.apacheHBase   % "hbase-client"                      % Versions.hbaseVersion   exclude ("org.slf4j", "slf4j-api"),
  Constants.apacheHBase   % "hbase-hadoop-compat"               % Versions.hbaseVersion,
  Constants.apacheHBase   % "hbase-hadoop-compat"               % Versions.hbaseVersion   classifier "tests",
  Constants.apacheHBase   % "hbase-hadoop2-compat"              % Versions.hbaseVersion,
  Constants.apacheHBase   % "hbase-hadoop2-compat"              % Versions.hbaseVersion   classifier "tests",
  Constants.apacheHBase   % "hbase-server"                      % Versions.hbaseVersion,
  Constants.apacheHBase   % "hbase-server"                      % Versions.hbaseVersion   classifier "tests",

  // Hadoop
  Constants.apacheHadoop  % "hadoop-common"                     % Versions.hadoopVersion,
  Constants.apacheHadoop  % "hadoop-common"                     % Versions.hadoopVersion  classifier "tests",
  Constants.apacheHadoop  % "hadoop-hdfs"                       % Versions.hadoopVersion,
  Constants.apacheHadoop  % "hadoop-hdfs"                       % Versions.hadoopVersion  classifier "tests",
  Constants.apacheHadoop  % "hadoop-mapreduce-client-core"      % Versions.hadoopVersion,
  Constants.apacheHadoop  % "hadoop-mapreduce-client-jobclient" % Versions.hadoopVersion
).map(_.excludeAll ( ExclusionRule("log4j", "log4j"), ExclusionRule ("org.slf4j", "slf4j-log4j12")))


lazy val devDeps: Seq[ModuleID] = Seq(
  // Akka
  "com.typesafe.akka"       %% "akka-actor"                     % "2.5.6",
  "com.typesafe.akka"       %% "akka-testkit"                   % "2.5.6"                 % Test,

  // CSV Parser
  "net.sf.opencsv"          % "opencsv"                         % "2.3",

  // Failsafe
  "net.jodah"               % "failsafe"                        % "1.0.4",

  // Metrics
  "com.google.guava"        % "guava"                           % "14.0.1",

  // Mockito
  "org.mockito"             % "mockito-core"                    % "2.10.0"                % "test",

  // logging
  "org.slf4j"               % "slf4j-api"                       % "1.7.25",
  "org.slf4j"               % "log4j-over-slf4j"                % "1.7.25",
  "ch.qos.logback"          % "logback-classic"                 % "1.2.3"                 % "test",

  // testing
  "org.scalactic"           %%  "scalactic"                     % "3.0.4",
  "org.scalatest"           %%  "scalatest"                     % "3.0.4"                 % "test",

  //junit
  "com.novocode"            % "junit-interface" % "0.11" % Test,
  "junit"                   % "junit" % "4.12" % Test

)

lazy val exTransiviveDeps: Seq[ExclusionRule] = Seq(
  ExclusionRule("commons-logging", "commons-logging"),
  ExclusionRule("log4j", "log4j"),
  ExclusionRule ("org.slf4j", "slf4j-log4j12")
)


moduleName := "sbr-admin-data-repository-hbase"
description := "<description>"
libraryDependencies ++= devDeps ++ hadoopDeps
//excludeDependencies ++= exTransiviveDeps

logBuffered in Test := false
crossPaths := false
testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a"))
