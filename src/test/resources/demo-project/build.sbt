name := "stringcalc"

scalaVersion := "3.2.0"

libraryDependencies ++= Seq(
  "com.michaelpollmeier" %% "scala-repl-pp" % "0.0.1"
)

enablePlugins(JavaAppPackaging)
