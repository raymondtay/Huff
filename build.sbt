/** 
 *  Huff 
 *
 *  @author : Raymond Tay
 *  @date : 18 Jan 2017
 */

// Akka main library dependencies
val akkaVersion = "2.4.16"
val AkkaDeps = Seq(
  "com.typesafe.akka" %% "akka-actor"                          ,
  "com.typesafe.akka" %% "akka-agent"                          ,
  "com.typesafe.akka" %% "akka-camel"                          ,
  "com.typesafe.akka" %% "akka-cluster"                        ,
  "com.typesafe.akka" %% "akka-cluster-metrics"                ,
  "com.typesafe.akka" %% "akka-cluster-sharding"               ,
  "com.typesafe.akka" %% "akka-cluster-tools"                  ,
  "com.typesafe.akka" %% "akka-contrib"                        ,
  "com.typesafe.akka" %% "akka-multi-node-testkit"             ,
  "com.typesafe.akka" %% "akka-osgi"                           ,
  "com.typesafe.akka" %% "akka-persistence"                    ,
  "com.typesafe.akka" %% "akka-persistence-tck"                ,
  "com.typesafe.akka" %% "akka-remote"                         ,
  "com.typesafe.akka" %% "akka-slf4j"                          ,
  "com.typesafe.akka" %% "akka-stream"                         ,
  "com.typesafe.akka" %% "akka-stream-testkit"                 ,
  "com.typesafe.akka" %% "akka-testkit"                        ,
  "com.typesafe.akka" %% "akka-distributed-data-experimental"  ,
  "com.typesafe.akka" %% "akka-typed-experimental"             ,
  "com.typesafe.akka" %% "akka-persistence-query-experimental" 
  ).map(_ % akkaVersion)

// Akka Http project library dependencies
val akkaHttpVersion = "10.0.1"
val AkkaHttpDeps = Seq(
  "com.typesafe.akka" %% "akka-http-core"       ,
  "com.typesafe.akka" %% "akka-http"            ,
  "com.typesafe.akka" %% "akka-http-testkit"    ,
  "com.typesafe.akka" %% "akka-http-spray-json" ,
  "com.typesafe.akka" %% "akka-http-jackson"    ,
  "com.typesafe.akka" %% "akka-http-xml"        
).map(_ % akkaHttpVersion)

// Cats , Circe and Eff library dependencies
val catsVersion = "0.9.0"
val effVersion  = "2.2.0"
val circeVersion = "0.6.1"

// to write types like Reader[String, ?]
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

// to get types like Reader[String, ?] (with more than one type parameter) correctly inferred for scala 2.11.8
addCompilerPlugin("com.milessabin" % "si2712fix-plugin_2.11.8" % "1.2.0")

val CatsEffDeps = Seq(
  "org.typelevel" %% "cats" % catsVersion,
  "org.atnos"     %% "eff"  % effVersion)

// light-weight json library
val CirceDeps = Seq(
  "io.circe"      %% "circe-core"    ,
  "io.circe"      %% "circe-generic" ,
  "io.circe"      %% "circe-parser"  
).map(_ % circeVersion)

// specs2 / scalacheck testing framework
val specs2Version = "3.8.5"
val TestingDeps = Seq(
  "org.specs2" %% "specs2-core"       ,
  "org.specs2" %% "specs2-scalacheck" 
).map(_ % specs2Version).map(_ % "test")

// scala logging framework
val scalaLoggerVersion = "3.5.0"
val ScalaLoggerDeps = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggerVersion,
  "ch.qos.logback" % "logback-core"    % "1.1.7",
  "ch.qos.logback" % "logback-classic" % "1.1.7"
)

// settings related to sbt-doctest
doctestTestFramework    := DoctestTestFramework.Specs2
doctestWithDependencies := false
doctestMarkdownEnabled  := true

// settings related to sbt-native-packager
enablePlugins(JavaServerAppPackaging) // enable the sbt-native-packager plugin
maintainer in Docker         := "Raymond Tay <rtay@deep-labs.com>"
packageSummary in Docker     := "Huff Cluster"
packageDescription in Docker := "Http service wrapped inside Akka cluster"

// project settings in sbt
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
      CatsEffDeps ++ CirceDeps ++ AkkaDeps ++ AkkaHttpDeps ++ ScalaLoggerDeps ++ TestingDeps,

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
    

