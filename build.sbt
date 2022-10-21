name := "scala-repl-pp"
organization := "com.michaelpollmeier"

scalaVersion := "3.2.0"

libraryDependencies ++= Seq(
  "com.michaelpollmeier" %% "scala3-compiler" % "3.2.0+1-extensible-repl",
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
  "org.scalatest"    %% "scalatest"    % "3.2.12" % Test,
)

resolvers += Resolver.mavenLocal
enablePlugins(JavaAppPackaging)
Global/onChangedBuildSource := ReloadOnSourceChanges

