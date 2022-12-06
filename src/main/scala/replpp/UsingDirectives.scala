package replpp

import java.nio.file.Path
import scala.collection.mutable

object UsingDirectives {
  val Prefix = "//> using"
  val LibDirective = s"$Prefix lib "
  val FileDirective = s"$Prefix file "

  def findImportedFilesRecursively(file: os.Path): Set[os.Path] = {
    def findImportedFilesRecursively0(file: os.Path, visited: Set[os.Path]): Set[os.Path] = {
      val files = scanFor(FileDirective, os.read.lines(file)).iterator.map(resolveFile(file, _)).toSet
      files ++ files.filterNot(visited.contains).flatMap { file =>
        findImportedFilesRecursively0(file, visited + file)
      }
    }

    findImportedFilesRecursively0(file, visited = Set.empty)
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
