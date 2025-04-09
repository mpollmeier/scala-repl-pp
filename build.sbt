name := "scala-repl-pp-root"
ThisBuild / organization := "com.michaelpollmeier"
publish/skip := true

val scalaVersions = Seq("3.5.2", "3.6.4")
ThisBuild/scalaVersion := scalaVersions.max

lazy val ScalaTestVersion = "3.2.18"
lazy val Slf4jVersion = "2.0.16"

lazy val core364 = project.in(file("core"))
  .dependsOn(shadedLibs)
  .enablePlugins(JavaAppPackaging)
  .settings(
    scalaVersion := "3.6.4",
    name := s"scala-repl-pp_${scalaVersion.value}",
    Compile/unmanagedSourceDirectories += (Compile/sourceDirectory).value / s"scala-${scalaVersion.value}",
    Compile/mainClass := Some("replpp.Main"),
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
      "org.slf4j"       % "slf4j-simple"    % Slf4jVersion % Optional,
    ),
    target := baseDirectory.value / "target_364",
    executableScriptName := "srp",
    commonSettings,
  )

lazy val core352 = project.in(file("core"))
  .dependsOn(shadedLibs)
  .enablePlugins(JavaAppPackaging)
  .settings(
    scalaVersion := "3.5.2",
    name := s"scala-repl-pp_${scalaVersion.value}",
    Compile/unmanagedSourceDirectories += (Compile/sourceDirectory).value / s"scala-${scalaVersion.value}",
    Compile/mainClass := Some("replpp.Main"),
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
      "org.slf4j"       % "slf4j-simple"    % Slf4jVersion % Optional,
    ),
    target := baseDirectory.value / "target_352",
    executableScriptName := "srp",
    commonSettings,
  )

lazy val shadedLibs = project.in(file("shaded-libs"))
  .settings(
    name := "scala-repl-pp-shaded-libs",
    scalaVersion := scalaVersions.min,
    Compile/compile/scalacOptions ++= Seq(
      "-language:implicitConversions",
      "-Wconf:any:silent", // silence warnings from shaded libraries
      "-explain"
    ),
    Compile/doc/scalacOptions += "-nowarn",
    commonSettings,
  )

lazy val server364 = project.in(file("server"))
  .dependsOn(core364)
  .enablePlugins(JavaAppPackaging)
  .settings(
    scalaVersion := "3.6.4",
    name := s"scala-repl-pp-server_${scalaVersion.value}",
    Compile/unmanagedSourceDirectories += (Compile/sourceDirectory).value / s"scala-${scalaVersion.value}",
    Compile/mainClass := Some("replpp.server.Main"),
    libraryDependencies ++= Seq(
      "com.lihaoyi"   %% "cask"         % "0.9.5",
      "org.slf4j"      % "slf4j-simple" % Slf4jVersion % Optional,
      "com.lihaoyi"   %% "requests"     % "0.8.2" % Test,
    ),
    target := baseDirectory.value / "target_364",
    executableScriptName := "srp-server",
    fork := true, // important: otherwise we run into classloader issues
    commonSettings,
  )

lazy val server352 = project.in(file("server"))
  .dependsOn(core352)
  .enablePlugins(JavaAppPackaging)
  .settings(
    scalaVersion := "3.5.2",
    name := s"scala-repl-pp-server_${scalaVersion.value}",
    Compile/unmanagedSourceDirectories += (Compile/sourceDirectory).value / s"scala-${scalaVersion.value}",
    Compile/mainClass := Some("replpp.server.Main"),
    libraryDependencies ++= Seq(
      "com.lihaoyi"   %% "cask"         % "0.9.5",
      "org.slf4j"      % "slf4j-simple" % Slf4jVersion % Optional,
      "com.lihaoyi"   %% "requests"     % "0.8.2" % Test,
    ),
    target := baseDirectory.value / "target_352",
    executableScriptName := "srp-server",
    fork := true, // important: otherwise we run into classloader issues
    commonSettings,
  )

lazy val integrationTests = project.in(file("integration-tests"))
  .dependsOn(server364)
  .settings(
    name := "integration-tests",
    fork := true, // important: otherwise we run into classloader issues
    libraryDependencies ++= Seq(
      "org.slf4j"      % "slf4j-simple" % Slf4jVersion % Optional,
      "org.scalatest" %% "scalatest"    % ScalaTestVersion % Test,
    ),
    publish/skip := true
  )

val commonSettings = Seq(
  maintainer.withRank(KeyRanks.Invisible) := "michael@michaelpollmeier.com",
)

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

ThisBuild/publishTo := sonatypePublishToBundle.value
ThisBuild/scmInfo := Some(ScmInfo(url("https://github.com/mpollmeier/scala-repl-pp"),
                            "scm:git@github.com:mpollmeier/scala-repl-pp.git"))
ThisBuild/homepage := Some(url("https://github.com/mpollmeier/scala-repl-pp/"))
ThisBuild/licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild/developers := List(
  Developer("mpollmeier", "Michael Pollmeier", "michael@michaelpollmeier.com", url("http://www.michaelpollmeier.com/"))
)

Global/onChangedBuildSource := ReloadOnSourceChanges
