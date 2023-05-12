package replpp

import java.nio.file.{Files, Path}
import scala.collection.immutable.Seq
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}

package object util {

  /** `to` is included in this Range */
  case class Range(from: Int, to: Int)

  def findAdjacentNumberRanges(numbers: Seq[Int]): Seq[Range] = {
    if (numbers.isEmpty) {
      Seq.empty
    } else {
      var start = numbers(0)
      var end = numbers(0)
      val ranges = Seq.newBuilder[Range]

      for (i <- 1 until numbers.length) {
        if (numbers(i) == end + 1) {
          end = numbers(i)
        } else {
          ranges += Range(start, end)
          start = numbers(i)
          end = numbers(i)
        }
      }

      ranges += Range(start, end)
      ranges.result()
    }
  }

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
