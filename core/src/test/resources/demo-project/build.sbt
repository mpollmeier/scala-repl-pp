name := "stringcalc"

scalaVersion := "3.6.4"
val srpVersion = "0.5.4"

libraryDependencies ++= Seq(
  "com.michaelpollmeier" % "scala-repl-pp" % srpVersion cross CrossVersion.full,
  "com.github.scopt" %% "scopt" % "4.1.0",
)

enablePlugins(JavaAppPackaging)
