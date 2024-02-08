package replpp

import replpp.shaded.fansi

import java.nio.file.{FileSystems, Files, Path}
import scala.collection.immutable.Seq
import scala.io.Source
import scala.util.{Try, Using}

package object util {

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
  def terminalWidth: Try[Int] = {
    Try {
      if (scala.util.Properties.isLinux)
        org.jline.terminal.impl.jna.linux.LinuxNativePty.current.getSize.getColumns
      else if (scala.util.Properties.isMac)
        org.jline.terminal.impl.jna.osx.OsXNativePty.current.getSize.getColumns
      else
      throw new NotImplementedError("terminal width lookup only supported for linux and mac for now")
    }
  }

}
