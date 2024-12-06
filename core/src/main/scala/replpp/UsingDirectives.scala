package replpp

import java.nio.file.{Files, Path}
import scala.collection.mutable

object UsingDirectives {
  private val Prefix    = "//> using"
  val LibDirective      = s"$Prefix dep "
  val FileDirective     = s"$Prefix file "
  val ResolverDirective = s"$Prefix resolver"
  val ClasspathDirective = s"$Prefix classpath"

  def findImportedFilesRecursively(path: Path, visited: Set[Path] = Set.empty): Seq[Path] = {
    val rootDir: Path =
      if (Files.isDirectory(path)) path
      else path.toAbsolutePath.getParent

    val importedFiles = findImportedFiles(util.linesFromFile(path), rootDir)
    val recursivelyImportedFiles = importedFiles.filterNot(visited.contains).flatMap { file =>
      findImportedFilesRecursively(file, visited + file)
    }
    (importedFiles ++ recursivelyImportedFiles).distinct
  }

  def findImportedFiles(lines: IterableOnce[String], rootPath: Path): Seq[Path] =
    scanFor(FileDirective, lines).iterator.map(resolveFile(rootPath, _)).toSeq

  def findDeclaredDependencies(lines: IterableOnce[String]): Seq[String] =
    scanFor(LibDirective, lines)

  def findResolvers(lines: IterableOnce[String]): Seq[String] =
    scanFor(ResolverDirective, lines)

  def findClasspathEntriesInLines(sourceLines: IterableOnce[String], relativeTo: Path): Seq[Path] = {
    for {
      classpathEntry <- scanFor(ClasspathDirective, sourceLines)
      pathRelativeToDeclaringFile = relativeTo.resolve(classpathEntry)
    } yield pathRelativeToDeclaringFile
  }

  def findClasspathEntriesInFiles(files: IterableOnce[Path]): Seq[Path] = {
    files.iterator.flatMap { file =>
      val dir = file.getParent
      findClasspathEntriesInLines(util.linesFromFile(file), dir)
    }.toSeq
  }

  private def scanFor(directive: String, lines: IterableOnce[String]): Seq[String] = {
    lines
      .iterator
      .map(_.trim)
      .filter(_.startsWith(directive))
      .map(_.drop(directive.length).trim)
      .toSeq
  }

}
