
/**
* PROJECT DEF
*/
lazy val `sbr-admin-data` = (project in file("."))
  .enablePlugins(BuildInfoPlugin, PlayScala)
  .disablePlugins(AssemblyPlugin)
  .dependsOn(`repository-hbase`)
  .aggregate(model, `repository-hbase`, `hbase-loader`)

lazy val `hbase-loader` = project
  .dependsOn(`repository-hbase`)

lazy val `repository-hbase` = project
  .disablePlugins(AssemblyPlugin)
  .dependsOn(model)

lazy val model = project
  .disablePlugins(AssemblyPlugin)

/**
* SETTINGS AND CONFIGURATION
*/
buildInfoPackage := "controllers"

coverageMinimum := 55

