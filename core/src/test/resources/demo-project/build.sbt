name := "stringcalc"

scalaVersion := "3.2.1"

libraryDependencies ++= Seq(
  "com.michaelpollmeier" %% "scala-repl-pp" % "0.0.29"
)

enablePlugins(JavaAppPackaging)
