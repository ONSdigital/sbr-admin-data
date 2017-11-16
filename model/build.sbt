name := "sbr-admin-data-model"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.4"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"
logBuffered in Test := false
libraryDependencies += "junit" % "junit" % "4.12" % Test
crossPaths := false
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test
testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a"))

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.0"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.0"
libraryDependencies += "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.9.0"
