name := "scala-repl-pp-root"
publish/skip := true

ThisBuild / organization := "com.michaelpollmeier"
ThisBuild / scalaVersion := "3.5.2"
lazy val ScalaTestVersion = "3.2.18"
lazy val Slf4jVersion = "2.0.16"

lazy val shadedLibs = project.in(file("shaded-libs"))
  .settings(
    name := "scala-repl-pp-shaded-libs",
    Compile/compile/scalacOptions ++= Seq(
      "-language:implicitConversions",
      "-Wconf:any:silent", // silence warnings from shaded libraries
      "-explain"
    ),
    Compile/doc/scalacOptions += "-nowarn",
    crossVersion := CrossVersion.full,
  )

lazy val core = project.in(file("core"))
  .dependsOn(shadedLibs)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "scala-repl-pp",
    Compile/mainClass := Some("replpp.Main"),
    executableScriptName := "srp",
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
      "org.slf4j"       % "slf4j-simple"    % Slf4jVersion % Optional,
    ),
    crossVersion := CrossVersion.full,
    assemblyJarName := "srp.jar", // TODO remove the '.jar' suffix - when doing so, it doesn't work any longer
  )

lazy val server = project.in(file("server"))
  .dependsOn(core)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "scala-repl-pp-server",
    executableScriptName := "srp-server",
    Compile/mainClass := Some("replpp.server.Main"),
    fork := true, // important: otherwise we run into classloader issues
    libraryDependencies ++= Seq(
      "com.lihaoyi"   %% "cask"         % "0.9.5",
      "org.slf4j"      % "slf4j-simple" % Slf4jVersion % Optional,
      "com.lihaoyi"   %% "requests"     % "0.8.2" % Test,
    ),
    crossVersion := CrossVersion.full,
  )

lazy val integrationTests = project.in(file("integration-tests"))
  .dependsOn(server)
  .settings(
    name := "integration-tests",
    fork := true, // important: otherwise we run into classloader issues
    libraryDependencies ++= Seq(
      "org.slf4j"      % "slf4j-simple" % Slf4jVersion % Optional,
      "org.scalatest" %% "scalatest"    % ScalaTestVersion % Test,
    ),
    publish/skip := true
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

ThisBuild/resolvers += Resolver.mavenLocal
Global/onChangedBuildSource := ReloadOnSourceChanges

ThisBuild/assemblyMergeStrategy := {
  case "META-INF/versions/9/module-info.class" => MergeStrategy.first
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}
ThisBuild/assemblyPrependShellScript := Some(sbtassembly.AssemblyPlugin.defaultShellScript)

ThisBuild/publishTo := sonatypePublishToBundle.value
ThisBuild/scmInfo := Some(ScmInfo(url("https://github.com/mpollmeier/scala-repl-pp"),
                            "scm:git@github.com:mpollmeier/scala-repl-pp.git"))
ThisBuild/homepage := Some(url("https://github.com/mpollmeier/scala-repl-pp/"))
ThisBuild/licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild/developers := List(
  Developer("mpollmeier", "Michael Pollmeier", "michael@michaelpollmeier.com", url("http://www.michaelpollmeier.com/"))
)
