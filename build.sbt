name := "scala-repl-pp-root"
ThisBuild / organization := "com.michaelpollmeier"
publish/skip := true

val defaultScalaVersion = "3.6.4"
val crossScalaVersions = Seq(defaultScalaVersion, "3.5.2")
ThisBuild/scalaVersion := defaultScalaVersion

lazy val ScalaTestVersion = "3.2.18"
lazy val Slf4jVersion = "2.0.16"

lazy val core2 = projectMatrix.in(file("core2"))
  .jvmPlatform(scalaVersions = crossScalaVersions)
  .settings(
    name := "core2",
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
    ),
    // Compile / unmanagedSourceDirectories += baseDirectory.value / "src/main/scala-3.6.4",
    // commonSettings
  )

// lazy val core = project.in(file("core"))
//   .dependsOn(shadedLibs)
//   .enablePlugins(JavaAppPackaging)
//   .settings(
//     name := "scala-repl-pp",
//     Compile/mainClass := Some("replpp.Main"),
//     executableScriptName := "srp",
//     libraryDependencies ++= Seq(
//       "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
//       "org.slf4j"       % "slf4j-simple"    % Slf4jVersion % Optional,
//     ),
//     commonSettings,
//   )

// lazy val shadedLibs = project.in(file("shaded-libs"))
//   .settings(
//     name := "scala-repl-pp-shaded-libs",
//     Compile/compile/scalacOptions ++= Seq(
//       "-language:implicitConversions",
//       "-Wconf:any:silent", // silence warnings from shaded libraries
//       "-explain"
//     ),
//     Compile/doc/scalacOptions += "-nowarn",
//     commonSettings,
//   )

// lazy val server = project.in(file("server"))
//   .dependsOn(core)
//   .enablePlugins(JavaAppPackaging)
//   .settings(
//     name := "scala-repl-pp-server",
//     executableScriptName := "srp-server",
//     Compile/mainClass := Some("replpp.server.Main"),
//     fork := true, // important: otherwise we run into classloader issues
//     libraryDependencies ++= Seq(
//       "com.lihaoyi"   %% "cask"         % "0.9.5",
//       "org.slf4j"      % "slf4j-simple" % Slf4jVersion % Optional,
//       "com.lihaoyi"   %% "requests"     % "0.8.2" % Test,
//     ),
//     commonSettings,
//   )

// lazy val integrationTests = project.in(file("integration-tests"))
//   .dependsOn(server)
//   .settings(
//     name := "integration-tests",
//     fork := true, // important: otherwise we run into classloader issues
//     libraryDependencies ++= Seq(
//       "org.slf4j"      % "slf4j-simple" % Slf4jVersion % Optional,
//       "org.scalatest" %% "scalatest"    % ScalaTestVersion % Test,
//     ),
//     publish/skip := true
//   )

// val commonSettings = Seq(
//   crossVersion := CrossVersion.full,
//   maintainer.withRank(KeyRanks.Invisible) := "michael@michaelpollmeier.com",
// )

ThisBuild / libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
  "com.lihaoyi"   %% "os-lib"    % "0.9.1" % Test,
)

ThisBuild / versionScheme := Some("strict")

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

// ThisBuild/publishTo := sonatypePublishToBundle.value
ThisBuild/scmInfo := Some(ScmInfo(url("https://github.com/mpollmeier/scala-repl-pp"),
                            "scm:git@github.com:mpollmeier/scala-repl-pp.git"))
ThisBuild/homepage := Some(url("https://github.com/mpollmeier/scala-repl-pp/"))
ThisBuild/licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild/developers := List(
  Developer("mpollmeier", "Michael Pollmeier", "michael@michaelpollmeier.com", url("http://www.michaelpollmeier.com/"))
)

Global/onChangedBuildSource := ReloadOnSourceChanges
