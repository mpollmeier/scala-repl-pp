name := "stringcalc"

scalaVersion := "3.3.0"

libraryDependencies ++= Seq(
  "com.michaelpollmeier" %% "scala-repl-pp" % "0.1.56"
)

enablePlugins(JavaAppPackaging)
