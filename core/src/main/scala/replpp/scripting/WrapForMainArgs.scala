package replpp.scripting

/** So that we can execute the given script and potentially handle parameters, we wrap it in some code using
 * https://github.com/com-lihaoyi/mainargs  */
object WrapForMainArgs {

  /** linesBeforeScript: allows us to adjust line numbers in error reporting... */
  case class WrappingResult(fullScript: String, linesBeforeRunBeforeCode: Int, linesBeforeScript: Int)

  def apply(scriptCode: String, runBefore: Seq[String], runAfter: Seq[String]): WrappingResult = {
    val wrapperCodeStart0 =
      s"""import replpp.shaded.mainargs
         |import mainargs.main // intentionally shadow any potentially given @main
         |
         |// ScriptingDriver expects an object with a predefined name and a main entrypoint method
         |object ${ScriptingDriver.MainClassName} {
         |// runBeforeCode START
         |""".stripMargin
    val linesBeforeRunBeforeCode =
      wrapperCodeStart0.lines().count().toInt
      + 1 // for the line break after `wrapperCodeStart0`

    val wrapperCodeStart1 =
      s"""$wrapperCodeStart0
         |${runBefore.mkString("\n")}
         |// runBeforeCode END
         |""".stripMargin

    var linesBeforeScript = 0 // to adjust line number reporting

    val mainImpl =
      if (scriptCode.contains("@main"))
        scriptCode
      else {
        linesBeforeScript += 1 // because we added the following line _before_ the wrapped script code
        s"""@main def _execMain(): Unit = {
           |$scriptCode
           |}""".stripMargin
      }

    linesBeforeScript += wrapperCodeStart1.lines().count().toInt
    linesBeforeScript += 1 // for the line break after `wrapperCodeStart1`
    val fullScript =
      s"""$wrapperCodeStart1
         |$mainImpl
         |
         |  def ${ScriptingDriver.MainMethodName}(args: Array[String]): Unit = {
         |    mainargs.ParserForMethods(this).runOrExit(args.toSeq)
         |
         |    ${runAfter.mkString("\n")}
         |  }
         |}
         |""".stripMargin

    WrappingResult(fullScript, linesBeforeRunBeforeCode, linesBeforeScript)
  }

}
