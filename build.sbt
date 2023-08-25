name := "root"
publish/skip := true

ThisBuild / organization := "com.michaelpollmeier"
ThisBuild / scalaVersion := "3.3.0"

lazy val ScalaCollectionCompatVersion = "2.11.0"
lazy val ScalaTestVersion = "3.2.16"

lazy val shadedLibs = project.in(file("shaded-libs"))
  .settings(
    name := "scala-repl-pp-shaded-libs",
    libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % ScalaCollectionCompatVersion,
    scalacOptions ++= Seq(
      "-language:implicitConversions",
      "-Wconf:any:silent", // silence warnings from shaded libraries
      "-explain"
    )
  )

lazy val core = project.in(file("core"))
  .dependsOn(shadedLibs)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "scala-repl-pp",
    Compile/mainClass := Some("replpp.Main"),
    executableScriptName := "scala-repl-pp",
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
    ),
  )

lazy val server = project.in(file("server"))
  .dependsOn(core)
  .configs(IntegrationTest)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "scala-repl-pp-server",
    Compile/mainClass := Some("replpp.server.Main"),
    Defaults.itSettings,
    fork := true, // important: otherwise we run into classloader issues
    libraryDependencies ++= Seq(
      "com.lihaoyi"   %% "cask"         % "0.8.3",
      "org.slf4j"      % "slf4j-simple" % "2.0.7" % Optional,
      "com.lihaoyi"   %% "requests"     % "0.8.0" % Test,
      "org.scalatest" %% "scalatest"    % ScalaTestVersion % "it",
    )
  )

ThisBuild / libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
  "com.lihaoyi"   %% "os-lib"    % "0.9.1" % Test,
)

ThisBuild / javacOptions ++= Seq(
  "-g", //debug symbols
  "--release", "11"
)

ThisBuild / scalacOptions ++= Seq(
  "-release", "11",
  "-deprecation",
  "-feature",
)

ThisBuild/Test/fork := false

ThisBuild/resolvers += Resolver.mavenLocal
Global/onChangedBuildSource := ReloadOnSourceChanges

ThisBuild/publishTo := sonatypePublishToBundle.value
ThisBuild/scmInfo := Some(ScmInfo(url("https://github.com/mpollmeier/scala-repl-pp"),
                            "scm:git@github.com:mpollmeier/scala-repl-pp.git"))
ThisBuild/homepage := Some(url("https://github.com/mpollmeier/scala-repl-pp/"))
ThisBuild/licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild/developers := List(
  Developer("mpollmeier", "Michael Pollmeier", "michael@michaelpollmeier.com", url("http://www.michaelpollmeier.com/"))
)
