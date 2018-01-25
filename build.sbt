
/**
* PROJECT DEF
*/
lazy val `sbr-admin-data` = (project in file("."))
  .enablePlugins(BuildInfoPlugin, PlayScala)
  .disablePlugins(AssemblyPlugin)
  .dependsOn(`repository-hbase`)
  .aggregate(model, `repository-hbase`, `hbase-loader-thin`, `hbase-loader-fat`)

lazy val `hbase-loader-thin` = project
  .dependsOn(`repository-hbase`)

lazy val `hbase-loader-fat` = project
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