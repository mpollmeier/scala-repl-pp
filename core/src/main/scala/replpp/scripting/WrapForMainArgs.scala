package replpp.scripting

/** So that we can execute the given script and potentially handle parameters, we wrap it in some code using
 * https://github.com/com-lihaoyi/mainargs  */
object WrapForMainArgs {

  /** linesBeforeWrappedCode: allows us to adjust line numbers in error reporting... */
  case class WrappingResult(fullScript: String, linesBeforeWrappedCode: Int)

  def apply(scriptCode: String, runBeforeAsSourceLines: Seq[String]): WrappingResult = {
    var linesBeforeWrappedCode = 0 // to adjust line number reporting

    val runBeforeCode = runBeforeAsSourceLines.mkString("\n")
    val wrapperCodeStart =
      s"""import replpp.shaded.mainargs
         |import mainargs.main // intentionally shadow any potentially given @main
         |
         |// ScriptingDriver expects an object with a predefined name and a main entrypoint method
         |object ${ScriptingDriver.MainClassName} {
         |$runBeforeCode
         |""".stripMargin

    val mainImpl =
      if (scriptCode.contains("@main"))
        scriptCode
      else {
        linesBeforeWrappedCode += 1 // because we added the following line _before_ the wrapped script code
        s"""@main def _execMain(): Unit = {
           |$scriptCode
           |}""".stripMargin
      }

    linesBeforeWrappedCode += wrapperCodeStart.lines().count().toInt
    val fullScript =
      s"""$wrapperCodeStart
         |$mainImpl
         |
         |  def ${ScriptingDriver.MainMethodName}(args: Array[String]): Unit = {
         |    mainargs.ParserForMethods(this).runOrExit(args.toSeq)
         |  }
         |}
         |""".stripMargin

    WrappingResult(fullScript, linesBeforeWrappedCode)
  }

}
