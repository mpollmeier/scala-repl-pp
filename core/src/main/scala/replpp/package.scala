import replpp.util.{ClasspathHelper, linesFromFile}

import java.io.File
import java.io.File.pathSeparator
import java.lang.System.lineSeparator
import java.net.URL
import java.nio.file.{Files, Path, Paths}
import scala.annotation.tailrec
import scala.collection.mutable
import scala.io.Source
import scala.util.Using

package object replpp {
  enum Colors { case BlackWhite, Default }

  val VerboseEnvVar    = "SCALA_REPL_PP_VERBOSE"

  /** The user's home directory */
  lazy val home: Path = Paths.get(System.getProperty("user.home"))

  /** The current working directory for this process. */
  lazy val pwd: Path = Paths.get(".").toAbsolutePath

  lazy val globalPredefFile = home.resolve(".scala-repl-pp.sc")
  lazy val globalPredefFileMaybe = Option(globalPredefFile).filter(Files.exists(_))
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

  def allPredefCode(config: Config): String =
    allPredefLines(config).mkString(lineSeparator)

  def allPredefLines(config: Config): Seq[String] = {
    val resultLines = Seq.newBuilder[String]
    val visited = mutable.Set.empty[Path]
    import config.colors

    resultLines ++= DefaultPredefLines

    val allPredefFiles = globalPredefFileMaybe ++ config.predefFiles
    allPredefFiles.foreach { file =>
      val importedFiles = UsingDirectives.findImportedFilesRecursively(file, visited.toSet)
      visited ++= importedFiles
      importedFiles.foreach { file =>
        resultLines ++= linesFromFile(file)
      }

      resultLines ++= linesFromFile(file)
      visited += file
    }

    config.scriptFile.foreach { file =>
      val importedFiles = UsingDirectives.findImportedFilesRecursively(file, visited.toSet)
      visited ++= importedFiles
      importedFiles.foreach { file =>
        resultLines ++= linesFromFile(file)
      }
    }

    resultLines.result().filterNot(_.trim.startsWith(UsingDirectives.FileDirective))
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
