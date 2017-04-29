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
// Announcements:
// 23 Jan 2017 - http://akka.io/news/2017/01/23/akka-http-10.0.2-security-fix-released.html
//               therefore, pushing to 10.0.2
//
val akkaHttpVersion = "10.0.2"
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
val circeVersion = "0.7.0"

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

// commandline parser
val CliDeps = Seq(
  "com.github.scopt" %% "scopt" % "3.5.0"
)

// settings related to sbt-doctest
doctestTestFramework    := DoctestTestFramework.Specs2
doctestWithDependencies := false
doctestMarkdownEnabled  := true

// Coverage enabled
// Note: Its rather nonsense to have to test whether the server starts up or not.
//       therefore they are disabled 
// Its important to set coverageEnabled := false when ready for packaging and distribution
// [info] The new value will be used by *:libraryDependencies, compile:compile::scalacOptions
// [info] Reapplying settings...
// [info] Set current project to Huff (in build file:/Users/tayboonl/Huff/)
coverageEnabled := false
coverageExcludedPackages := "deeplabs\\.http\\.RestServer;deeplabs\\.cluster\\.Huff.*"

// Scalaconfig library
val ScalaCfg = Seq("com.github.andr83" %% "scalaconfig" % "0.3")

val ConsulDeps = Seq("com.orbitz.consul" % "consul-client" % "0.13.10")
resolvers += "bintray" at "http://jcenter.bintray.com"

// 
// Project settings in sbt
// 
val project = Project(
  id = "Huff",
  base = file(".")
  ).enablePlugins(JavaServerAppPackaging,UniversalPlugin).settings(
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
      CatsEffDeps ++ CliDeps ++ CirceDeps ++ AkkaDeps ++ AkkaHttpDeps ++ ConsulDeps ++ ScalaCfg ++ ScalaLoggerDeps ++ TestingDeps,

    javaOptions in run ++= Seq( // javaOptions when project is being run
      "-Xms1024m",
      "-Xmx4096m",
      "-XX:+UseParallelGC", 
      "-server", 
      "-XX:+UseCompressedOops",
      "-Djava.library.path=./target/native",
      s"-Dcom.sun.management.jmxremote.port=${sys.env.getOrElse("JMX_REMOTE_PORT", default = "9999")}",
      s"-Dcom.sun.management.jmxremote.ssl=${sys.env.getOrElse("JMX_REMOTE_SSL", default = "false")}",
      s"-Dcom.sun.management.jmxremote.authenticate=${sys.env.getOrElse("JMX_REMOTE_AUTHENTICATE", default = "false")}"
      ),
    javaOptions in Universal := (javaOptions in run).value, // propagate `run` settings to packaged scripts
    Keys.fork in run := true,
    mainClass in (Compile, run) := Some("deeplabs.cluster.Huff")
  ) // end of project's settings
    

