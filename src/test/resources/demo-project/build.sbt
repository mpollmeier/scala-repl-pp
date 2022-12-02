name := "stringcalc"

scalaVersion := "3.2.1"

libraryDependencies ++= Seq(
  "com.michaelpollmeier" %% "scala-repl-pp" % "0.0.12+18-b5c108ab"
)

enablePlugins(JavaAppPackaging)
