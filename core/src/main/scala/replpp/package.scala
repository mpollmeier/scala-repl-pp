import java.io.File
import java.lang.System.lineSeparator
import java.net.URL
import java.nio.file.Path
import scala.annotation.tailrec

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
    val fromDependencies = dependencyFiles(config).mkString(pathSeparator)
    val fromClassLoaderHierarchy =
      jarsFromClassLoaderRecursively(classOf[replpp.ReplDriver].getClassLoader)
        .map(_.getFile)
        .mkString(pathSeparator)

    Seq(fromClassLoaderHierarchy, fromDependencies, fromJavaClassPathProperty).mkString(pathSeparator)
  }

  private def dependencyFiles(config: Config): Seq[File] = {
    val predefCode = allPredefCode(config)
    val scriptCode = config.scriptFile.map(os.read).getOrElse("")
    val allDependencies = config.dependencies ++
      UsingDirectives.findDeclaredDependencies(s"$predefCode\n$scriptCode")
    Dependencies.resolveOptimistically(allDependencies, verboseEnabled(config))
  }

  private def jarsFromClassLoaderRecursively(classLoader: ClassLoader): Seq[URL] = {
    classLoader match {
      case cl: java.net.URLClassLoader =>
        jarsFromClassLoaderRecursively(cl.getParent) ++ cl.getURLs
      case _ => Seq.empty
    }
  }

  def allPredefCode(config: Config): String = {
    val importedFiles = {
      val fromPredefCode = config.predefCode.map { code =>
        UsingDirectives.findImportedFiles(code.split(lineSeparator), os.pwd)
      }.getOrElse(Seq.empty)
      val fromFiles = (config.scriptFile.toSeq ++ config.predefFiles)
        .flatMap(UsingDirectives.findImportedFilesRecursively)
        .reverse // dependencies should get evaluated before dependents
      fromPredefCode ++ fromFiles
    }

    // --predefCode, ~/.scala-repl-pp.sc and `SCALA_REPL_PP_PREDEF_CODE` env var
    val fromPredefCode =
      Seq.concat(
        config.predefCode,
        Option(System.getenv(PredefCodeEnvVar)).filter(_.nonEmpty),
        readGlobalPredefFile
      ).map((os.pwd, _))

    val fromPredefFiles = (config.predefFiles ++ importedFiles).map { file =>
      (file, os.read(file))
    }

    val predefCodeByFile =
      if (config.predefFilesBeforePredefCode)
        fromPredefFiles ++ fromPredefCode
      else
        fromPredefCode ++ fromPredefFiles

    predefCodeByFile.map(_._2).distinct.mkString(lineSeparator)
  }

  /**
    * resolve absolute or relative paths to an absolute path
    * - if given pathStr is an absolute path, just take that
    * - if it's a relative path, use given base path to resolve it to an absolute path
    */
  def resolveFile(base: os.Path, pathStr: String): os.Path = {
    if (Path.of(pathStr).isAbsolute) os.Path(pathStr)
    else base / os.RelPath(pathStr)
  }

  private def readGlobalPredefFile: Seq[String] = {
    if (os.exists(globalPredefFile))
      os.read.lines(globalPredefFile)
    else
      Seq.empty
  }

  // ":" on unix
  val pathSeparator = java.io.File.pathSeparator
}
