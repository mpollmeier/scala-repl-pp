package replpp

import java.nio.file.{Files, Path}
import scala.collection.immutable.Seq
import scala.io.Source
import scala.jdk.CollectionConverters.*
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

}
