package replpp

import java.nio.file.Path
import scala.collection.mutable

object UsingDirectives {
  val LibDirective = "//> using lib "
  val FileDirective = "//> using file "

  def findImportedFilesRecursively(lines: IterableOnce[String], visited: Set[os.Path] = Set.empty): Set[os.Path] = {
    val files = scanFor(FileDirective, lines).iterator.map { pathStr =>
      if (Path.of(pathStr).isAbsolute) os.Path(pathStr)
      else os.pwd / os.RelPath(pathStr)
    }.toSet
    files ++ files.filterNot(visited.contains).map(os.read.lines).flatMap { lines =>
      findImportedFilesRecursively(lines, visited ++ files)
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
