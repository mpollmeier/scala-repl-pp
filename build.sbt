name := "root"
publish/skip := true

ThisBuild / organization := "com.michaelpollmeier"
ThisBuild / scalaVersion := "3.2.1"

lazy val core   = project.in(file("core"))
lazy val server = project.in(file("server")).dependsOn(core)
lazy val all    = project.in(file("all")).dependsOn(core, server)

ThisBuild / libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
)

ThisBuild/Test/fork := true

resolvers += Resolver.mavenLocal
enablePlugins(JavaAppPackaging)
Global/onChangedBuildSource := ReloadOnSourceChanges

publishTo := sonatypePublishToBundle.value
scmInfo := Some(ScmInfo(url("https://github.com/mpollmeier/scala-repl-pp"),
                            "scm:git@github.com:mpollmeier/scala-repl-pp.git"))
homepage := Some(url("https://github.com/mpollmeier/scala-repl-pp/"))
licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
developers := List(
  Developer("mpollmeier", "Michael Pollmeier", "michael@michaelpollmeier.com", url("http://www.michaelpollmeier.com/"))
)
