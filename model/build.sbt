moduleName := "sbr-admin-data-hbase.model"

crossPaths := false


libraryDependencies ++= Seq (
  filters,
  ws,
  // scala-date
  "com.github.nscala-time"          %%  "nscala-time"              %  "2.16.0",

  "org.scalactic"                   %%  "scalactic"                %  "3.0.4",
  "org.scalatest"                   %%  "scalatest"                %  "3.0.4"    % "test"
)