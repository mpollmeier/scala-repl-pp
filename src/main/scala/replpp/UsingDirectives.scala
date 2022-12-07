package replpp

import scala.collection.mutable

object UsingDirectives {
  val Prefix = "//> using"
  val LibDirective = s"$Prefix lib "
  val FileDirective = s"$Prefix file "

  def findImportedFilesRecursively(file: os.Path): Seq[os.Path] = {
    def findImportedFilesRecursively0(file: os.Path, visited: Set[os.Path]): Seq[os.Path] = {
      val importedFiles = findImportedFiles(os.read.lines(file), file)
      val recursivelyImportedFiles = importedFiles.filterNot(visited.contains).flatMap { file =>
        findImportedFilesRecursively0(file, visited + file)
      }
      importedFiles ++ recursivelyImportedFiles
    }

    findImportedFilesRecursively0(file, visited = Set.empty)
  }

  def findImportedFiles(lines: IterableOnce[String], rootPath: os.Path): Seq[os.Path] =
    scanFor(FileDirective, lines).iterator.map(resolveFile(rootPath, _)).toSeq

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
