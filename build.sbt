name := "scala-repl-pp"
organization := "com.michaelpollmeier"
scalaVersion := "3.2.2-RC1-bin-20221020-3649818-NIGHTLY"

libraryDependencies ++= Seq(
  "org.scala-lang"   %% "scala3-compiler" % scalaVersion.value,
  "com.lihaoyi"      %% "mainargs" % "0.3.0",
  "com.lihaoyi"      %% "os-lib"   % "0.8.1",
  "com.lihaoyi"      %% "pprint"   % "0.7.3",
  "com.github.scopt" %% "scopt"    % "4.1.0",
  ("io.get-coursier" %% "coursier" % "2.0.13").cross(CrossVersion.for3Use2_13)
    .exclude("org.scala-lang.modules", "scala-xml_2.13")
    .exclude("org.scala-lang.modules", "scala-collection-compat_2.13"),

  "org.slf4j"        %  "slf4j-simple" % "1.7.36" % Test,
  "org.scalatest"    %% "scalatest"    % "3.2.12" % Test,
)

resolvers += Resolver.mavenLocal
enablePlugins(JavaAppPackaging)
Global/onChangedBuildSource := ReloadOnSourceChanges

