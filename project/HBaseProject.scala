import sbt.Keys.{crossPaths, dependencyOverrides, mainClass, packageBin, resolvers}
import sbt.{ExclusionRule, ModuleID, _}
import sbtassembly.AssemblyPlugin.autoImport.{assembly, MergeStrategy, assemblyMergeStrategy}

import scoverage.ScoverageKeys.coverageMinimum

object HBaseProject {

  lazy val settings = Seq(
    resolvers += "cloudera" at "https://repository.cloudera.com/cloudera/cloudera-repos/",
    mainClass in (Compile, packageBin) := Some("hbase.load.BulkLoader"),
    crossPaths := false,
    dependencyOverrides += "com.google.guava" % "guava" % "14.0.1",
  //  coverageMinimum := 27
    assemblyMergeStrategy in assembly := {
      case "mrapp-generated-classpath" => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )

  lazy val hbaseClientDependencies = Seq(
    hbase.groupId  % "hbase-common" % hbase.version,
    hbase.groupId  % "hbase-client" % hbase.version exclude ("org.slf4j", "slf4j-api"),
    hadoop.groupId % "hadoop-common" % hadoop.version
  )

  lazy val hadoopDependencies: Seq[ModuleID] = hbaseClientDependencies ++ Seq(
    // HBase
    hbase.groupId   % "hbase-common"                      % hbase.version   classifier "tests",
    hbase.groupId   % "hbase-hadoop-compat"               % hbase.version,
    hbase.groupId   % "hbase-hadoop-compat"               % hbase.version   classifier "tests",
    hbase.groupId   % "hbase-hadoop2-compat"              % hbase.version,
    hbase.groupId   % "hbase-hadoop2-compat"              % hbase.version   classifier "tests",
    hbase.groupId   % "hbase-server"                      % hbase.version,
    hbase.groupId   % "hbase-server"                      % hbase.version   classifier "tests",

    // Hadoop
    hadoop.groupId  % "hadoop-common"                     % hadoop.version  classifier "tests",
    hadoop.groupId  % "hadoop-hdfs"                       % hadoop.version  exclude ("commons-daemon", "commons-daemon"),
    hadoop.groupId  % "hadoop-hdfs"                       % hadoop.version  classifier "tests",
    hadoop.groupId  % "hadoop-mapreduce-client-core"      % hadoop.version,
    hadoop.groupId  % "hadoop-mapreduce-client-jobclient" % hadoop.version
  ).map(_.excludeAll(
    ExclusionRule("log4j", "log4j"),
    ExclusionRule ("org.slf4j", "slf4j-log4j12"),
    ExclusionRule("javax.xml.stream", "stax-api"),
    ExclusionRule("org.mortbay.jetty", "servlet-api-2.5"),
    ExclusionRule("org.mortbay.jetty", "jsp-api-2.1"),
    ExclusionRule("io.netty", "netty-all")
  ))

  private[this] lazy val hbase = BasicModuleDetail("org.apache.hbase", "1.2.0-cdh5.10.1")

  private[this] lazy val hadoop = BasicModuleDetail("org.apache.hadoop", "2.6.0-cdh5.10.1")

  case class BasicModuleDetail(groupId: String, version: String)

}
