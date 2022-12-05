package replpp

import java.nio.file.Path
import scala.collection.mutable

object UsingDirectives {
  val Prefix = "//> using"
  val LibDirective = s"$Prefix lib "
  val FileDirective = s"$Prefix file "

  def findImportedFilesRecursively(file: os.Path, visited: Set[os.Path] = Set.empty): Set[os.Path] = {
    val files = scanFor(FileDirective, os.read.lines(file)).iterator.map { pathStr =>
      if (Path.of(pathStr).isAbsolute) os.Path(pathStr)
      else file / os.RelPath(pathStr)
    }.toSet
    files ++ files.filterNot(visited.contains).flatMap { file =>
      findImportedFilesRecursively(file, visited + file)
    }
  }

  def findDeclaredDependencies(source: String): IterableOnce[String] =
    scanFor(LibDirective, source.linesIterator)

  private def scanFor(directive: String, lines: IterableOnce[String]): IterableOnce[String] = {
    lines
      .iterator
      .map(_.trim)
      .filter(_.startsWith(directive))
      .map(_.drop(directive.length).trim)
  }

}
