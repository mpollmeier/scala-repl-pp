package replpp

/**
 * originally imported from https://github.com/com-lihaoyi/Ammonite/blob/151446c55e763b3b8c0152226f617ad1a0d719d4/amm/util/src/main/scala/ammonite/util/Imports.scala
 */


/**
  * Represents the importing of a single name in the Ammonite REPL, of the
  * form
  *
  * {{{
  * import $prefix.{$fromName => $toName}
  * }}}
  *
  * All imports are reduced to this form; `import $prefix.$name` is results in
  * the `fromName` and `toName` being the same, while `import $prefix._` or
  * `import $prefix.{foo, bar, baz}` are split into multiple distinct
  * [[ImportData]] objects.
  *
  * Note that imports can be of one of three distinct `ImportType`s: importing
  * a type, a term, or both. This lets us properly deal with shadowing correctly
  * if we import the type and term of the same name from different places
  */
case class ImportData(fromName: Name,
                      toName: Name,
                      prefix: Seq[Name],
                      importType: ImportData.ImportType)


object ImportData{
  sealed case class ImportType(name: String)
  val Type = ImportType("Type")
  val Term = ImportType("Term")
  val TermType = ImportType("TermType")

  def apply(name: String, importType: ImportType = Term): Seq[ImportData] = {
    val elements = name.split('.')
    assert(elements.nonEmpty)

    val simpleNames =
      if (elements.last.startsWith("{") && elements.last.endsWith("}"))
        elements.last.stripPrefix("{").stripSuffix("}").split(',').map(_.trim).toSeq
      else
        Seq(elements.last)

    simpleNames.map { simpleName =>
      ImportData(
        Name(simpleName),
        Name(simpleName),
        Name("_root_") :: elements.init.map(Name(_)).toList,
        importType
      )
    }
  }
}
