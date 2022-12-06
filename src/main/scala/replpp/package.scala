import java.lang.System.lineSeparator
import java.nio.file.Path

package object replpp {

  def compilerArgs(config: Config, predefCode: String): Array[String] = {
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

  def predefCodeByFile(config: Config): Seq[(os.Path, String)] = {
    val fromPredefCodeParam = config.predefCode.map((os.pwd, _)).toSeq
    val importedFiles = {
      val filesToSearch = config.scriptFile.toSeq ++ config.predefFiles
      filesToSearch.flatMap(UsingDirectives.findImportedFilesRecursively)
    }
    (config.predefFiles ++ importedFiles).map { file =>
      (file, os.read.lines(file).mkString(lineSeparator))
    } ++ fromPredefCodeParam
  }

  def allPredefCode(config: Config): String =
    predefCodeByFile(config).map(_._2).mkString(lineSeparator)

  /**
    * resolve absolute or relative paths to an absolute path
    * - if given pathStr is an absolute path, just take that
    * - if it's a relative path, use given base path to resolve it to an absolute path
    */
  def resolveFile(base: os.Path, pathStr: String): os.Path = {
    if (Path.of(pathStr).isAbsolute) os.Path(pathStr)
    else base / os.RelPath(pathStr)
  }
}
