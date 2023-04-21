package replpp

import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.jdk.CollectionConverters.IteratorHasAsScala

object UsingDirectives {
  val Prefix = "//> using"
  val LibDirective = s"$Prefix lib "
  val FileDirective = s"$Prefix file "

  def findImportedFilesRecursively(path: Path, visited: Set[Path] = Set.empty): Seq[Path] = {
    val rootDir: Path =
      if (Files.isDirectory(path)) path
      else path.getParent

    val importedFiles = findImportedFiles(Files.lines(path).iterator.asScala, rootDir)
    val recursivelyImportedFiles = importedFiles.filterNot(visited.contains).flatMap { file =>
      findImportedFilesRecursively(file, visited + file)
    }
    (importedFiles ++ recursivelyImportedFiles).distinct
  }

  def findImportedFiles(lines: IterableOnce[String], rootPath: Path): Seq[Path] =
    scanFor(FileDirective, lines).iterator.map(resolveFile(rootPath, _)).toSeq
    
  def findImportedFilesRecursively(lines: IterableOnce[String], rootPath: Path): Seq[Path] = {
    val results = Seq.newBuilder[Path]
    val visited = mutable.Set.empty[Path]

    findImportedFiles(lines, rootPath).foreach { file =>
      results += file
      visited += file

      val recursiveFiles = findImportedFilesRecursively(file, visited.toSet)
      results ++= recursiveFiles
      visited ++= recursiveFiles
    }

    results.result().distinct
  }

  def findDeclaredDependencies(source: String): Seq[String] =
    scanFor(LibDirective, source.linesIterator)

  private def scanFor(directive: String, lines: IterableOnce[String]): Seq[String] = {
    lines
      .iterator
      .map(_.trim)
      .filter(_.startsWith(directive))
      .map(_.drop(directive.length).trim)
      .toSeq
  }

}
