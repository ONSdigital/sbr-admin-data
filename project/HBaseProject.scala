import sbt.Keys.{crossPaths, dependencyOverrides, mainClass, packageBin, resolvers}
import sbt.{ExclusionRule, ModuleID, _}
import sbtassembly.AssemblyPlugin.autoImport.{MergeStrategy, assembly, assemblyMergeStrategy}

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
    hbaseModule("hbase-client") exclude ("org.slf4j", "slf4j-api"),
    hbaseModule("hbase-common"),
    hadoopModule("hadoop-common")
  ).map(_.excludeAll(exclusionRules :_*))

  lazy val hbaseDependencies = Seq(
    // HBase
    hbaseModule("hbase-common") classifier "tests",
    hbaseModule("hbase-hadoop-compat"),
    hbaseModule("hbase-hadoop-compat") classifier "tests",
    hbaseModule("hbase-hadoop2-compat"),
    hbaseModule("hbase-hadoop2-compat") classifier "tests",
    hbaseModule("hbase-server"),
    hbaseModule("hbase-server") classifier "tests",

    // Hadoop
    hadoopModule("hadoop-common") classifier "tests",
    hadoopModule("hadoop-hdfs") exclude ("commons-daemon", "commons-daemon"),
    hadoopModule("hadoop-hdfs") classifier "tests",
    hadoopModule("hadoop-mapreduce-client-core"),
    hadoopModule("hadoop-mapreduce-client-jobclient")
  ).map(_.excludeAll(exclusionRules :_*))

  lazy val hbaseAllDependencies = hbaseClientDependencies ++ hbaseDependencies

  private def hbaseModule(artifactName: String): ModuleID = {
    "org.apache.hbase"  % artifactName % "1.2.0-cdh5.10.1"
  }

  private def hadoopModule(artifactName: String): ModuleID = {
    "org.apache.hadoop" % artifactName % "2.6.0-cdh5.10.1"
  }

  private[this] lazy val exclusionRules = Seq(
    ExclusionRule("log4j", "log4j"),
    ExclusionRule ("org.slf4j", "slf4j-log4j12"),
    ExclusionRule("javax.xml.stream", "stax-api"),
    ExclusionRule("org.mortbay.jetty", "servlet-api-2.5"),
    ExclusionRule("org.mortbay.jetty", "jsp-api-2.1"),
    ExclusionRule("io.netty", "netty-all")
  )

}
