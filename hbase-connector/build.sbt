
/**
  * PROJECT DEF
  */
moduleName := "sbr-admin-data-hbase-connector"
description := "<description>"
resolvers += "cloudera" at "https://repository.cloudera.com/cloudera/cloudera-repos/"
crossPaths := false


/**
  * DEPENDENCIES LISTINGS
  */

libraryDependencies ++= Seq(
  ws,

  "io.netty"                %  "netty-all"                      % "4.0.51.Final",

  //@NOTE - patch for unresolved dependency
  "commons-daemon"          %  "commons-daemon"                 % "1.0.13",

  // scala-date
  "com.github.nscala-time"  %% "nscala-time"                    % "2.16.0",

  // Akka
  "com.typesafe.akka"       %% "akka-actor"                     % "2.5.6",
  "com.typesafe.akka"       %% "akka-testkit"                   % "2.5.6"                  % Test,

  // CSV Parser
  "net.sf.opencsv"          % "opencsv"                         % "2.3",

  // Failsafe
  "net.jodah"               % "failsafe"                        % "1.0.4",

  // Mockito
  "org.mockito"             % "mockito-core"                    % "2.10.0"                 % "test",

  // logging
  "org.slf4j"               % "slf4j-api"                       % "1.7.25",
  "org.slf4j"               % "log4j-over-slf4j"                % "1.7.25",
  "ch.qos.logback"          % "logback-classic"                 % "1.2.3"                  % "test",

  // testing
  "org.scalactic"           %%  "scalactic"                     % "3.0.4",
  "org.scalatest"           %%  "scalatest"                     % "3.0.4"                  % "test",

  "io.lemonlabs"            %%  "scala-uri"                     % "0.5.0",

  //junit
  "com.novocode"            % "junit-interface"                 % "0.11"                   % Test,
  "junit"                   % "junit"                           % "4.12"                   % Test
).map(_.excludeAll(
    ExclusionRule("log4j", "log4j"),
    ExclusionRule ("org.slf4j", "slf4j-log4j12"),
    ExclusionRule("javax.xml.stream", "stax-api"),
    ExclusionRule("org.mortbay.jetty", "servlet-api-2.5"),
    ExclusionRule("org.mortbay.jetty", "jsp-api-2.1"),
    ExclusionRule("io.netty", "netty-common"),
    ExclusionRule("io.netty", "netty-handler"),
    ExclusionRule("io.netty", "netty-codec"),
    ExclusionRule("io.netty", "netty-transport"),
    ExclusionRule("io.netty", "netty-transport-native-epoll"),
    ExclusionRule("io.netty", "netty-buffer")
  )
) ++ HBaseModuleSettings.hadoopDependencies.map(module =>
      sys.env.get("DB_IN_MEMORY") match {
        case Some(v) if v.equals("true") => module
        case _ => module % Provided
      }
)


// Metrics
dependencyOverrides += "com.google.guava"        % "guava"                           % "14.0.1"

coverageMinimum := 27
