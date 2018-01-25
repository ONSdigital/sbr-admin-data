
/**
  * PROJECT DEF
  */
moduleName := "sbr-admin-data-hbase-loader-fat"
description := "HBase bulk loader with all necessary hadoop dependencies"
libraryDependencies ++= HBaseModuleSettings.hadoopDependencies
resolvers += "cloudera" at "https://repository.cloudera.com/cloudera/cloudera-repos/"
mainClass in (Compile, packageBin) := Some("hbase.load.BulkLoader")

crossPaths := false

dependencyOverrides += "com.google.guava" % "guava" % "14.0.1"

coverageMinimum := 27

assemblyMergeStrategy in assembly := {
  case "mrapp-generated-classpath" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}