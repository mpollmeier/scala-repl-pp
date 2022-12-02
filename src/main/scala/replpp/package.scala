import java.lang.System.lineSeparator

package object replpp {

  def compilerArgs(config: Config): Array[String] = {
    val predefCode = allPredefCode(config)
    val scriptCode = config.scriptFile.map(os.read).getOrElse("")
    val allDependencies = config.dependencies ++
      UsingDirectives.findDeclaredDependencies(s"$predefCode\n$scriptCode")

    val compilerArgs = Array.newBuilder[String]

    val dependencyFiles = Dependencies.resolveOptimistically(allDependencies, config.verbose)
    compilerArgs ++= Array("-classpath", replClasspath(dependencyFiles))
    compilerArgs += "-explain" // verbose scalac error messages
    compilerArgs += "-deprecation"
    if (config.nocolors) compilerArgs ++= Array("-color", "never")
    compilerArgs.result()
  }

  private def replClasspath(dependencies: Seq[java.io.File]): String = {
    val inheritedClasspath = System.getProperty("java.class.path")
    val separator = System.getProperty("path.separator")

    val entriesForDeps = dependencies.mkString(separator)
    s"$inheritedClasspath$separator$entriesForDeps"
  }
  
  def allPredefCode(config: Config): String = {
    val predefLines: Seq[String] = config.predefCode.getOrElse("") +: readAdditionalFiles(config.predefFiles)
    val scriptLines = config.scriptFile.map(os.read.lines(_)).getOrElse(Seq.empty)
    val importedFiles = UsingDirectives.findImportedFiles(predefLines ++ scriptLines)
    val linesViaUsingDirective = readAdditionalFiles(importedFiles)
    (predefLines ++ linesViaUsingDirective).mkString(lineSeparator)
  }

  def readAdditionalFiles(files: IterableOnce[os.Path]): Seq[String] = {
    files.iterator.distinct.flatMap { file =>
      assert(os.exists(file), s"$file does not exist")
      val lines = os.read.lines(file)
      println(s"importing $file (${lines.size} lines) from $file")
      lines
    }.toSeq
  }

}
