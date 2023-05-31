name := "root"
publish/skip := true

ThisBuild / organization := "com.michaelpollmeier"
ThisBuild / scalaVersion := "3.3.0"

lazy val ScalaTestVersion = "3.2.15"

lazy val core = project.in(file("core")).settings(
  name := "scala-repl-pp",
  libraryDependencies ++= Seq(
    "org.scala-lang"   %% "scala3-compiler" % scalaVersion.value,
    "com.lihaoyi"      %% "mainargs"  % "0.5.0",
    "com.lihaoyi"      %% "pprint"    % "0.8.1",
    "com.github.scopt" %% "scopt"     % "4.1.0",
    ("io.get-coursier" %% "coursier"  % "2.1.4").cross(CrossVersion.for3Use2_13)
      .exclude("org.scala-lang.modules", "scala-xml_2.13")
      .exclude("org.scala-lang.modules", "scala-collection-compat_2.13"),
    "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
  )
)

lazy val server = project.in(file("server"))
  .dependsOn(core)
  .configs(IntegrationTest)
  .settings(
    name := "scala-repl-pp-server",
    Defaults.itSettings,
    fork := true, // important: otherwise we run into classloader issues
    libraryDependencies ++= Seq(
      "com.lihaoyi"   %% "cask"         % "0.8.3",
      "org.slf4j"      % "slf4j-simple" % "2.0.7" % Optional,
      "com.lihaoyi"   %% "requests"     % "0.8.0" % Test,
      "org.scalatest" %% "scalatest"    % ScalaTestVersion % "it",
    )
  )

lazy val all = project.in(file("all"))
  .dependsOn(core, server)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "scala-repl-pp-all",
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.7" % Optional,
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
