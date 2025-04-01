package replpp

import dotty.tools.dotc.core.Contexts.Context
import java.nio.file.{FileSystems, Files, Path}
import replpp.shaded.fansi
import scala.collection.immutable.Seq
import scala.io.Source
import scala.util.{Try, Using}

package object util {
  def currentWorkingDirectory = Path.of(".")

  def sequenceTry[A](tries: Seq[Try[A]]): Try[Seq[A]] = {
    tries.foldRight(Try(Seq.empty[A])) {
      case (next, accumulator) => 
        for {
          a <- next
          acc <- accumulator 
        } yield a +: acc
    }
  }

  def linesFromFile(path: Path): Seq[String] =
    Using.resource(Source.fromFile(path.toFile))(_.getLines.toSeq)

  def deleteRecursively(path: Path): Unit = {
    if (Files.isDirectory(path))
      Files.list(path).forEach(deleteRecursively)

    Files.deleteIfExists(path)
  }

  def readFileFromZip(zipFile: Path, fileName: String): Try[Array[Byte]] = {
    Using(FileSystems.newFileSystem(zipFile, null)) { fileSystem =>
      Files.readAllBytes(fileSystem.getPath(fileName))
    }
  }

  def colorise(value: String, color: fansi.EscapeAttr)(using colors: Colors): String = {
    colors match {
      case Colors.BlackWhite => value
      case Colors.Default => color(value).render
    }
  }

  /** Lookup the current terminal width - useful e.g. if you want to render something using the maximum space available.
    * Uses jline-jna, which therefor needs to be in the repl's classpath, which is why it's listed in
    * {{{replpp.Config.ForClasspath.DefaultInheritClasspathIncludes}}} */
  def terminalWidth: Try[Int] =
    Using(org.jline.terminal.TerminalBuilder.terminal)(_.getWidth)

  def pathAsString(path: Path): String =
    path.toAbsolutePath.toString

}
