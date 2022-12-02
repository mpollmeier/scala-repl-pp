package replpp

object UsingDirectives {
  val LibDirective = "//> using lib "
  val FileDirective = "//> using file "

  def findImportedFiles(lines: IterableOnce[String]): Set[os.Path] =
    scanFor(FileDirective, lines).iterator.map(os.Path(_)).toSet

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
