name := "stringcalc"

scalaVersion := "3.2.1"

libraryDependencies ++= Seq(
  "com.michaelpollmeier" %% "scala-repl-pp" % "0.0.14"
)

enablePlugins(JavaAppPackaging)
