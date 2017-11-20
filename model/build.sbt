moduleName := "sbr-admin-data-model"

logBuffered in Test := false
libraryDependencies += "junit" % "junit" % "4.12" % Test
crossPaths := false
testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a"))

libraryDependencies ++= Seq (
  filters,
  // scala-date
  "com.github.nscala-time"          %%  "nscala-time"              %  "2.16.0",

  "org.scalactic"                   %%  "scalactic"                %  "3.0.4",
  "org.scalatest"                   %%  "scalatest"                %  "3.0.4"    % "test",
  "org.scalatestplus.play"          %%  "scalatestplus-play"       %  "2.0.0"    % Test,
  "com.fasterxml.jackson.core"      %   "jackson-core"             %  "2.9.0",
  "com.fasterxml.jackson.core"      %   "jackson-databind"         %  "2.9.0",
  "com.fasterxml.jackson.datatype"  %   "jackson-datatype-jsr310"  %  "2.9.0"
)