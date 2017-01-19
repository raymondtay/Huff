/** 
 *  Huff 
 *
 *  @author : Raymond Tay
 *  @date : 18 Jan 2017
 */

// Akka main library dependencies
val akkaVersion = "2.4.16"
val AkkaDeps = Seq(
  "com.typesafe.akka" %% "akka-actor"                          % akkaVersion,
  "com.typesafe.akka" %% "akka-agent"                          % akkaVersion,
  "com.typesafe.akka" %% "akka-camel"                          % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster"                        % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics"                % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding"               % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools"                  % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib"                        % akkaVersion,
  "com.typesafe.akka" %% "akka-multi-node-testkit"             % akkaVersion,
  "com.typesafe.akka" %% "akka-osgi"                           % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence"                    % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-tck"                % akkaVersion,
  "com.typesafe.akka" %% "akka-remote"                         % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j"                          % akkaVersion,
  "com.typesafe.akka" %% "akka-stream"                         % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit"                 % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit"                        % akkaVersion,
  "com.typesafe.akka" %% "akka-distributed-data-experimental"  % akkaVersion,
  "com.typesafe.akka" %% "akka-typed-experimental"             % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query-experimental" % akkaVersion
  )

// Akka Http project library dependencies
val akkaHttpVersion = "10.0.1"
val AkkaHttpDeps = Seq(
  "com.typesafe.akka" %% "akka-http-core"       % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-jackson"    % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion
)

// Cats and Eff library dependencies
val catsVersion = "0.9.0"
val effVersion  = "2.2.0"

// to write types like Reader[String, ?]
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

// to get types like Reader[String, ?] (with more than one type parameter) correctly inferred for scala 2.11.8
addCompilerPlugin("com.milessabin" % "si2712fix-plugin_2.11.8" % "1.2.0")

val CatsEffDeps = Seq(
  "org.typelevel" %% "cats" % catsVersion,
  "org.atnos"     %% "eff"  % effVersion
)

val specs2Version = "3.8.5"
val TestingDeps = Seq(
  "org.specs2" %% "specs2-core"       % specs2Version % "test",
  "org.specs2" %% "specs2-scalacheck" % specs2Version % "test"
)

doctestTestFramework    := DoctestTestFramework.Specs2
doctestWithDependencies := false
doctestMarkdownEnabled  := true

val project = Project(
  id = "Huff",
  base = file(".")
  ).settings(
    name         := "Huff",
    version      := "0.9",
    scalaVersion := "2.11.8",
    scalacOptions in Compile ++=  // scalac options when project is being compiled
      Seq(
        "-encoding",
        "UTF-8",
        "-target:jvm-1.8",
        "-deprecation",
        "-feature",
        "-unchecked",
        "-Xlog-reflective-calls",
        "-Xlint" 
        ),
    javacOptions in Compile ++=  // javac options when project is being compiled
      Seq(
        "-source", "1.8",
        "-target", "1.8",
        "-Xlint:unchecked",
        "-Xlint:deprecation"),
    libraryDependencies ++= 
      CatsEffDeps ++ AkkaDeps ++ AkkaHttpDeps ++ TestingDeps,

    javaOptions in run ++= Seq( // javaOptions when project is being run
      "-Xms1024m",
      "-Xmx4096m",
      "-XX:+UseParallelGC", 
      "-server", 
      "-XX:+UseCompressedOops",
      "-Djava.library.path=./target/native",
      s"-Dcom.sun.management.jmxremote.port=${sys.props.getOrElse("JMX_REMOTE_PORT", default = "9999")}",
      s"-Dcom.sun.management.jmxremote.ssl=${sys.props.getOrElse("JMX_REMOTE_SSL", default = "false")}",
      s"-Dcom.sun.management.jmxremote.authenticate=${sys.props.getOrElse("JMX_REMOTE_AUTHENTICATE", default = "false")}"
      ),
    Keys.fork in run := true,
    mainClass in (Compile, run) := Some("deeplabs.cluster.Huff")
  ) // end of project's settings
    

