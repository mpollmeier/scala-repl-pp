import replpp.util.{ClasspathHelper, SimpleDriver, linesFromFile}

import java.lang.System.lineSeparator
import java.nio.file.{Files, Path, Paths}
import scala.collection.mutable

package object replpp {
  enum Colors { case BlackWhite, Default }
  val VerboseEnvVar    = "SCALA_REPL_PP_VERBOSE"
  lazy val pwd: Path = Paths.get(".").toAbsolutePath
  lazy val home: Path = Paths.get(System.getProperty("user.home"))
  lazy val globalPredefFile = home.resolve(".scala-repl-pp.sc")
  lazy val globalPredefFileMaybe = Option(globalPredefFile).filter(Files.exists(_))

  def createTemporaryFileWithDefaultPredef(using colors: Colors): Path = {
    val file = Files.createTempFile("scala-repl-pp-default-predef", "sc")
    Files.writeString(file, DefaultPredefLines.mkString(lineSeparator))
    file
  }

  private[replpp] def DefaultPredefLines(using colors: Colors) = {
    val colorsImport = colors match {
      case Colors.BlackWhite => "replpp.Colors.BlackWhite"
      case Colors.Default => "replpp.Colors.Default"
    }
    Seq(
      "import replpp.Operators.*",
      s"given replpp.Colors = $colorsImport"
    )
  }

  private[replpp] def DefaultPredef(using Colors) = DefaultPredefLines.mkString(lineSeparator)

  /** verbose mode can either be enabled via the config, or the environment variable `SCALA_REPL_PP_VERBOSE=true` */
  def verboseEnabled(config: Config): Boolean = {
    config.verbose ||
      sys.env.get(VerboseEnvVar).getOrElse("false").toLowerCase.trim == "true"
  }

  def compilerArgs(config: Config): Array[String] = {
    val compilerArgs = Array.newBuilder[String]
    compilerArgs ++= Array("-classpath", ClasspathHelper.create(config))
    compilerArgs += "-explain" // verbose scalac error messages
    compilerArgs += "-deprecation"
    if (config.nocolors) compilerArgs ++= Array("-color", "never")

    compilerArgs.result()
  }

  def allPredefFiles(config: Config): Seq[Path] = {
    val allPredefFiles  = mutable.Set.empty[Path]
    allPredefFiles += createTemporaryFileWithDefaultPredef(using config.colors)
    allPredefFiles ++= config.predefFiles
    globalPredefFileMaybe.foreach(allPredefFiles.addOne)

    // the directly resolved predef files might reference additional files via `using` directive
    val predefFilesDirect = allPredefFiles.toSet
    predefFilesDirect.foreach { file =>
      val importedFiles = UsingDirectives.findImportedFilesRecursively(file, visited = allPredefFiles.toSet)
      allPredefFiles ++= importedFiles
    }

    // the script (if any) might also reference additional files via `using` directive
    config.scriptFile.foreach { file =>
      val importedFiles = UsingDirectives.findImportedFilesRecursively(file, visited = allPredefFiles.toSet)
      allPredefFiles ++= importedFiles
    }

    allPredefFiles.toSeq.sorted
  }

  def allPredefLines(config: Config): Seq[String] =
    allPredefFiles(config).flatMap(linesFromFile)

  /** precompile given predef files (if any) and update Config to include the results in the classpath */
  def precompilePredefFiles(config: Config): Config = {
    if (config.predefFiles.nonEmpty) {
      val predefClassfilesDir = new SimpleDriver().compile(
        replpp.compilerArgs(config),
        inputFiles = config.predefFiles,
        verbose = config.verbose
      ).get
      config.withAdditionalClasspathEntry(predefClassfilesDir)
    } else config
  }

  /**
    * resolve absolute or relative paths to an absolute path
    * - if given pathStr is an absolute path, just take that
    * - if it's a relative path, use given base path to resolve it to an absolute path
    * - if the base path is a file, take it's root directory - anything else doesn't make any sense.
    */
  def resolveFile(base: Path, pathStr: String): Path = {
    val path = Paths.get(pathStr)
    if (path.isAbsolute) path
    else {
      val base0 =
        if (Files.isDirectory(base)) base
        else base.getParent
      base0.resolve(path)
    }
  }

}
