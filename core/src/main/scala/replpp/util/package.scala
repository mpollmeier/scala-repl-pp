package replpp

import java.nio.file.{FileSystems, Files, Path}
import scala.collection.immutable.Seq
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}
import replpp.shaded.fansi

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

}
