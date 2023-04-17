import java.io.File
import java.lang.System.lineSeparator
import java.net.URL
import java.nio.file.Path
import scala.annotation.tailrec
import scala.collection.mutable

package object replpp {
  val PredefCodeEnvVar = "SCALA_REPL_PP_PREDEF_CODE"
  val VerboseEnvVar    = "SCALA_REPL_PP_VERBOSE"
  lazy val globalPredefFile = os.home / ".scala-repl-pp.sc"

  /** verbose mode can either be enabled via the config, or the environment variable `SCALA_REPL_PP_VERBOSE=true` */
  def verboseEnabled(config: Config): Boolean = {
    config.verbose ||
      sys.env.get(VerboseEnvVar).getOrElse("false").toLowerCase.trim == "true"
  }

  def compilerArgs(config: Config): Array[String] = {
    val compilerArgs = Array.newBuilder[String]
    compilerArgs ++= Array("-classpath", classpath(config))
    compilerArgs += "-explain" // verbose scalac error messages
    compilerArgs += "-deprecation"
    if (config.nocolors) compilerArgs ++= Array("-color", "never")
    compilerArgs.result()
  }

  def classpath(config: Config): String = {
    val fromJavaClassPathProperty = System.getProperty("java.class.path")
    val fromDependencies = dependencyFiles(config)

    if (verboseEnabled(config)) {
      println(s"resolved dependencies - adding ${fromDependencies.size} artifact(s) to classpath:")
      fromDependencies.foreach(println)
    }
    val fromClassLoaderHierarchy =
      jarsFromClassLoaderRecursively(classOf[replpp.ReplDriver].getClassLoader)
        .map(_.getFile)
        .mkString(pathSeparator)

    (fromClassLoaderHierarchy +: fromDependencies :+ fromJavaClassPathProperty).mkString(pathSeparator)
  }

  private def dependencyFiles(config: Config): Seq[File] = {
    val predefCode = allPredefCode(config)
    val scriptCode = config.scriptFile.map(os.read).getOrElse("")
    val allDependencies = config.dependencies ++
      UsingDirectives.findDeclaredDependencies(s"$predefCode\n$scriptCode")
    // TODO get back to work
//    Dependencies.resolve(allDependencies, verboseEnabled(config))
    ???
  }

  private def jarsFromClassLoaderRecursively(classLoader: ClassLoader): Seq[URL] = {
    classLoader match {
      case cl: java.net.URLClassLoader =>
        jarsFromClassLoaderRecursively(cl.getParent) ++ cl.getURLs
      case _ =>
        Seq.empty
    }
  }

  def allPredefCode(config: Config): String = {
    val resultLines = Seq.newBuilder[String]
    val visited = mutable.Set.empty[os.Path]

    def handlePredefCodeWithoutPredefFiles() = {
      val codeLines =
        lines(config.predefCode) ++                       // `--predefCode` parameter
        lines(Option(System.getenv(PredefCodeEnvVar))) ++ // ~/.scala-repl-pp.sc file
        globalPredefFileLines                             // `SCALA_REPL_PP_PREDEF_CODE` env var

      val importedFiles = UsingDirectives.findImportedFilesRecursively(codeLines, os.pwd)
      importedFiles.foreach { file =>
        resultLines ++= os.read.lines(file)
      }
      visited ++= importedFiles
      resultLines ++= codeLines
    }

    def handlePredefFiles() = {
      config.predefFiles.foreach { file =>
        val importedFiles = UsingDirectives.findImportedFilesRecursively(file, visited.toSet)
        visited ++= importedFiles
        importedFiles.foreach { file =>
          resultLines ++= os.read.lines(file)
        }

        resultLines ++= os.read.lines(file)
        visited += file
      }
    }

    if (config.predefFilesBeforePredefCode) {
      handlePredefFiles()
      handlePredefCodeWithoutPredefFiles()
    } else {
      handlePredefCodeWithoutPredefFiles()
      handlePredefFiles()
    }

    config.scriptFile.foreach { file =>
      val importedFiles = UsingDirectives.findImportedFilesRecursively(file, visited.toSet)
      visited ++= importedFiles
      importedFiles.foreach { file =>
        resultLines ++= os.read.lines(file)
      }
    }

    resultLines.result()
      .filterNot(_.trim.startsWith(UsingDirectives.FileDirective))
      .mkString(lineSeparator)
  }

  private def lines(str: String): Seq[String] =
    str.split(lineSeparator)

  private def lines(strMaybe: Option[String]): Seq[String] =
    strMaybe.map(lines).getOrElse(Seq.empty)

  /**
    * resolve absolute or relative paths to an absolute path
    * - if given pathStr is an absolute path, just take that
    * - if it's a relative path, use given base path to resolve it to an absolute path
    */
  def resolveFile(base: os.Path, pathStr: String): os.Path = {
    if (Path.of(pathStr).isAbsolute) os.Path(pathStr)
    else base / os.RelPath(pathStr)
  }

  private def globalPredefFileLines: Seq[String] = {
    if (os.exists(globalPredefFile))
      os.read.lines(globalPredefFile)
    else
      Seq.empty
  }

  // ":" on unix
  val pathSeparator = java.io.File.pathSeparator
}
