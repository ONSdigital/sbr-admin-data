moduleName := "sbr-admin-data-hbase.model"

logBuffered in Test := false
//libraryDependencies += "junit" % "junit" % "4.12" % Test
crossPaths := false
//testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a"))


libraryDependencies ++= Seq (
  filters,
  ws,
  // scala-date
  "com.github.nscala-time"          %%  "nscala-time"              %  "2.16.0",

  "org.scalactic"                   %%  "scalactic"                %  "3.0.4",
  "org.scalatest"                   %%  "scalatest"                %  "3.0.4"    % "test"
//  "com.fasterxml.jackson.core"      %   "jackson-core"             %  "2.9.0",
//  "com.fasterxml.jackson.core"      %   "jackson-databind"         %  "2.9.0",
//  "com.fasterxml.jackson.datatype"  %   "jackson-datatype-jsr310"  %  "2.9.0"
)