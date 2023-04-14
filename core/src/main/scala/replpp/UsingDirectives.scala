package replpp

import scala.collection.mutable

object UsingDirectives {
  val Prefix = "//> using"
  val LibDirective = s"$Prefix lib "
  val FileDirective = s"$Prefix file "

  def findImportedFilesRecursively(file: os.Path, visited: Set[os.Path] = Set.empty): Seq[os.Path] = {
    val rootDir: os.Path =
      if (os.isDir(file)) file
      else os.Path(file.toNIO.getParent)

    val importedFiles = findImportedFiles(os.read.lines(file), rootDir)
    val recursivelyImportedFiles = importedFiles.filterNot(visited.contains).flatMap { file =>
      findImportedFilesRecursively(file, visited + file)
    }
    importedFiles ++ recursivelyImportedFiles
  }

  def findImportedFiles(lines: IterableOnce[String], rootPath: os.Path): Seq[os.Path] =
    scanFor(FileDirective, lines).iterator.map(resolveFile(rootPath, _)).toSeq
    
  def findImportedFilesRecursively(lines: IterableOnce[String], rootPath: os.Path): Seq[os.Path] = {
    val results = Seq.newBuilder[os.Path]
    val visited = mutable.Set.empty[os.Path]

    findImportedFiles(lines, rootPath).foreach { file =>
      results += file

      val recursiveFiles = findImportedFilesRecursively(file, visited.toSet)
      results ++= recursiveFiles
      visited += file
      visited ++= recursiveFiles
    }

    results.result()
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
