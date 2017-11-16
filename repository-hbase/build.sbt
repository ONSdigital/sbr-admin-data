name := "sbr-admin-data-repository-hbase"
mainClass in (Compile, packageBin) := Some("hbase.load.BulkLoader")

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.4"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"
logBuffered in Test := false
libraryDependencies += "junit" % "junit" % "4.12" % Test
crossPaths := false
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test
testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a"))

val hbaseVersion = "1.3.1"
val hadoopVersion = "2.5.1"

// HBase
libraryDependencies += "org.apache.hbase" % "hbase-common" % hbaseVersion exclude("log4j", "log4j") exclude ("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.apache.hbase" % "hbase-common" % hbaseVersion classifier "tests" exclude("log4j", "log4j") exclude ("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.apache.hbase" % "hbase-client" % hbaseVersion exclude("log4j", "log4j")  exclude ("org.slf4j", "slf4j-log4j12") exclude ("org.slf4j", "slf4j-api")
libraryDependencies += "org.apache.hbase" % "hbase-hadoop-compat" % hbaseVersion exclude("log4j", "log4j") exclude ("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.apache.hbase" % "hbase-hadoop-compat" % hbaseVersion classifier "tests" exclude("log4j", "log4j") exclude ("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.apache.hbase" % "hbase-hadoop2-compat" % hbaseVersion exclude("log4j", "log4j") exclude ("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.apache.hbase" % "hbase-hadoop2-compat" % hbaseVersion classifier "tests" exclude("log4j", "log4j") exclude ("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.apache.hbase" % "hbase-server" % hbaseVersion exclude("log4j", "log4j") exclude ("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.apache.hbase" % "hbase-server" % hbaseVersion classifier "tests" exclude("log4j", "log4j") exclude ("org.slf4j", "slf4j-log4j12")

// Hadoop
libraryDependencies += "org.apache.hadoop" % "hadoop-common" % hadoopVersion exclude("log4j", "log4j") exclude ("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.apache.hadoop" % "hadoop-common" % hadoopVersion classifier "tests" exclude("log4j", "log4j") exclude ("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion exclude("log4j", "log4j") exclude ("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion classifier "tests" exclude("log4j", "log4j") exclude ("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.apache.hadoop" % "hadoop-mapreduce-client-core" % hadoopVersion exclude ("org.slf4j", "slf4j-log4j12")
libraryDependencies += "org.apache.hadoop" % "hadoop-mapreduce-client-jobclient" % hadoopVersion exclude("log4j", "log4j") exclude ("org.slf4j", "slf4j-log4j12")

// Akka
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.6",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.6" % Test
)

// CSV Parser
libraryDependencies += "net.sf.opencsv" % "opencsv" % "2.3"

// Failsafe
libraryDependencies += "net.jodah" % "failsafe" % "1.0.4"

// Metrics
dependencyOverrides += "com.google.guava" % "guava" % "14.0.1"

// Mockito
libraryDependencies += "org.mockito" % "mockito-core" % "2.10.0" % "test"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25"
libraryDependencies += "org.slf4j" % "log4j-over-slf4j" % "1.7.25"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % "test"
      