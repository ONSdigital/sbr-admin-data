
/**
* PROJECT DEF
*/
lazy val `sbr-admin-data` = (project in file("."))
  .enablePlugins(BuildInfoPlugin, PlayScala)
  .disablePlugins(AssemblyPlugin)
  .dependsOn(model)
  .dependsOn(`hbase-connector`)
  .aggregate(model, `hbase-connector`, `hbase-loader`)

lazy val model = project
  .disablePlugins(AssemblyPlugin)

//lazy val `repository-hbase` = project
//  .dependsOn(model)

lazy val `hbase-connector` = project
  .disablePlugins(AssemblyPlugin)
  .dependsOn(model)

lazy val `hbase-loader` = project
  .dependsOn(`hbase-connector`)

/**
* SETTINGS AND CONFIGURATION
*/
buildInfoPackage := "controllers"

coverageMinimum := 55

