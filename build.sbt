
/**
* PROJECT DEF
*/
lazy val `sbr-admin-data` = (project in file("."))
  .enablePlugins(BuildInfoPlugin, PlayScala)
  .disablePlugins(AssemblyPlugin)
  .dependsOn(`repository-hbase`)
  .aggregate(model, `repository-hbase`, `hbase-loader`, `hbase-loader-with-hadoop`)

lazy val `hbase-loader-with-hadoop` = project
  .settings(HBaseProject.settings)
  .dependsOn(`repository-hbase`)

lazy val `hbase-loader` = project
  .settings(HBaseProject.settings)
  .dependsOn(`repository-hbase`)

lazy val `repository-hbase` = project
  .disablePlugins(AssemblyPlugin)
  .dependsOn(model)

lazy val model = project
  .disablePlugins(AssemblyPlugin)

/**
* SETTINGS AND CONFIGURATION
*/
routesImport += "extensions.Binders._"

buildInfoPackage := "controllers"

coverageMinimum := 55

dockerBaseImage := "openjdk:8-jre"

dockerExposedPorts := Seq(9000)

libraryDependencies ++= HBaseProject.hbaseDependencies

resolvers += "cloudera" at "https://repository.cloudera.com/cloudera/cloudera-repos/"