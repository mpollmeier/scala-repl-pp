name := "scala-repl-pp"
ThisBuild/organization := "com.michaelpollmeier"
ThisBuild/scalaVersion := "3.2.2-RC1-bin-20221020-3649818-NIGHTLY"

ThisBuild/libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-simple" % "1.7.36" % Test,
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
)

resolvers += Resolver.mavenLocal

Global/onChangedBuildSource := ReloadOnSourceChanges

