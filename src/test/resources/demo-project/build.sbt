name := "stringcalc"
scalaVersion := "3.2.2-RC1-bin-20221020-3649818-NIGHTLY"

libraryDependencies ++= Seq(
  "com.michaelpollmeier" %% "scala-repl-pp" % "0.0.0+16-ff6cbfae+20221021-1150"
)

enablePlugins(JavaAppPackaging)
