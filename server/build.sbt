name := "scala-repl-pp-server"

libraryDependencies ++= Seq(
  "com.lihaoyi"      %% "cask"      % "0.8.3",
//   "org.slf4j"         % "slf4j-api" % "1.7.36",
  "org.slf4j" % "slf4j-simple" % "1.7.36" % Optional,
  "com.lihaoyi"      %% "requests"  % "0.7.1" % Test,
)
