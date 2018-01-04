
/**
* PROJECT DEF
*/
lazy val `sbr-admin-data` = (project in file("."))
  .enablePlugins(BuildInfoPlugin, GitVersioning, GitBranchPrompt, PlayScala)
  .enablePlugins(AssemblyPlugin)
  .settings(OnsDefaultsPlugin.buildInfoSettings)
  .dependsOn(model)
  .dependsOn(`repository-hbase`)
  .aggregate(model, `repository-hbase`)

lazy val model = project
  .disablePlugins(AssemblyPlugin)

lazy val `repository-hbase` = project
  .disablePlugins(AssemblyPlugin)
  .dependsOn(model)


/**
* SETTINGS AND CONFIGURATION
*/
buildInfoPackage := "controllers"

coverageMinimum := 55

