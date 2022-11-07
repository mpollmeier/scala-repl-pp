name := "scala-repl-pp"
organization := "com.michaelpollmeier"

scalaVersion := "3.2.1"

libraryDependencies ++= Seq(
  "com.michaelpollmeier" %% "scala3-compiler" % "3.2.1+1-extensible-repl",
  "com.lihaoyi"      %% "mainargs"  % "0.3.0",
  "com.lihaoyi"      %% "os-lib"    % "0.8.1",
  "com.lihaoyi"      %% "pprint"    % "0.7.3",
  "com.lihaoyi"      %% "cask"      % "0.8.3",
  "com.github.scopt" %% "scopt"     % "4.1.0",
  "org.slf4j"         % "slf4j-api" % "1.7.36",
  ("io.get-coursier" %% "coursier" % "2.0.13").cross(CrossVersion.for3Use2_13)
    .exclude("org.scala-lang.modules", "scala-xml_2.13")
    .exclude("org.scala-lang.modules", "scala-collection-compat_2.13"),
  "org.scala-lang.modules" %% "scala-xml" % "2.1.0",
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.8.1",

  "org.slf4j"        %  "slf4j-simple" % "1.7.36" % Optional,
  "com.lihaoyi"      %% "requests"  % "0.7.1" % Test,
  "org.scalatest"    %% "scalatest"    % "3.2.12" % Test,
)

resolvers += Resolver.mavenLocal
enablePlugins(JavaAppPackaging)
Global/onChangedBuildSource := ReloadOnSourceChanges

Test/fork := true

publishTo := sonatypePublishToBundle.value
scmInfo := Some(ScmInfo(url("https://github.com/mpollmeier/scala-repl-pp"),
                            "scm:git@github.com:mpollmeier/scala-repl-pp.git"))
homepage := Some(url("https://github.com/mpollmeier/scala-repl-pp/"))
licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
developers := List(
  Developer("mpollmeier", "Michael Pollmeier", "michael@michaelpollmeier.com", url("http://www.michaelpollmeier.com/"))
)
