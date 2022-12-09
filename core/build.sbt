name := "scala-repl-pp"

libraryDependencies ++= Seq(
  /* my fork was merged into upstream dotty, i.e. we'll be able to depend on the regular
   * scala3-compiler from org.scala-lang with the next major release, probably 3.3.0
   * see https://github.com/lampepfl/dotty/pull/16276 */
  "com.michaelpollmeier" %% "scala3-compiler" % "3.2.1+1-extensible-repl",
  "com.lihaoyi"      %% "mainargs"  % "0.3.0",
  "com.lihaoyi"      %% "os-lib"    % "0.8.1",
  "com.lihaoyi"      %% "pprint"    % "0.7.3",
  "com.github.scopt" %% "scopt"     % "4.1.0",
  // "org.slf4j"         % "slf4j-api" % "1.7.36",
  ("io.get-coursier" %% "coursier" % "2.0.13").cross(CrossVersion.for3Use2_13)
    .exclude("org.scala-lang.modules", "scala-xml_2.13")
    .exclude("org.scala-lang.modules", "scala-collection-compat_2.13"),
  "org.scala-lang.modules" %% "scala-xml" % "2.1.0",
)

