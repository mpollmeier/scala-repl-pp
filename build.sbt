name := "root"
publish/skip := true

ThisBuild / organization := "com.michaelpollmeier"
ThisBuild / scalaVersion := "3.2.1"

lazy val core   = project.in(file("core")).settings(
  name := "scala-repl-pp",
  libraryDependencies ++= Seq(
    /* my fork was merged into upstream dotty, i.e. we'll be able to depend on the regular
    * scala3-compiler from org.scala-lang with the next major release, probably 3.3.0
    * see https://github.com/lampepfl/dotty/pull/16276 */
    "com.michaelpollmeier" %% "scala3-compiler" % "3.2.1+1-extensible-repl",
    "com.lihaoyi"      %% "mainargs"  % "0.3.0",
    "com.lihaoyi"      %% "os-lib"    % "0.8.1",
    "com.lihaoyi"      %% "pprint"    % "0.7.3",
    "com.github.scopt" %% "scopt"     % "4.1.0",
    ("io.get-coursier" %% "coursier" % "2.0.13").cross(CrossVersion.for3Use2_13)
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
    libraryDependencies ++= Seq(
      "com.lihaoyi"      %% "cask"      % "0.8.3",
      "org.slf4j" % "slf4j-simple" % "1.7.36" % Optional,
      "com.lihaoyi"      %% "requests"  % "0.7.1" % Test,
      "org.scalatest" %% "scalatest" % "3.2.12" % "it",
    )
  )

lazy val all = project.in(file("all"))
  .dependsOn(core, server)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "scala-repl-pp-all"
  )

ThisBuild / libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
)

ThisBuild/Test/fork := true
ThisBuild/IntegrationTest/fork := true

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
