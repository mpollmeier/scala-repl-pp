package replpp.scripting

import java.util.stream.Collectors
import replpp.Config
import replpp.allPredefCode
import scala.collection.immutable.{AbstractSeq, LinearSeq}
import scala.jdk.CollectionConverters.*
import scala.xml.NodeSeq

/**
  * TODO rename and document. this one doesn't fork/spawn a new JVM and doesn't work in all setups. e.g. when starting from `sbt test` with `fork := false` it runs into classloader/classpath issues
  */
object ScriptRunner {

  def main(args: Array[String]): Unit = {
    val config = Config.parse(args)
    exec(config)
  }

  def exec(config: Config): Unit = {
    val scriptFile = config.scriptFile.getOrElse(throw new AssertionError("script file not defined - please specify e.g. via `--script=myscript.sc`"))
    if (!os.exists(scriptFile)) {
      throw new AssertionError(s"given script file $scriptFile does not exist")
    }

    val paramsInfoMaybe =
      if (config.params.nonEmpty) s" with params=${config.params}"
      else ""
    System.err.println(s"executing $scriptFile$paramsInfoMaybe")
    val scriptArgs: Seq[String] = {
      val commandArgs = config.command.toList
      val parameterArgs = config.params.flatMap { case (key, value) => Seq(s"--$key", value) }
      commandArgs ++ parameterArgs
    }

    // Predef code may include import statements... I didn't find a nice way to add them to the context of the
    // script file, so instead we'll just write it to the beginning of the script file.
    // That's obviously suboptimal, e.g. because it messes with the line numbers.
    // Therefor, we'll display the temp script file name to the user and not delete it, in case the script errors.
    val predefCode = allPredefCode(config)
    val predefPlusScriptFileTmp = os.temp(prefix = "scala-repl-pp-script-with-predef", suffix = ".sc", deleteOnExit = false)
    val scriptCode = os.read(scriptFile)
    val scriptContent = wrapForMainargs(predefCode, scriptCode)
    os.write.over(predefPlusScriptFileTmp, scriptContent)

    val compilerArgs = replpp.compilerArgs(config) :+ "-nowarn"
    new ScriptingDriver(
      compilerArgs = compilerArgs,
      scriptFile = predefPlusScriptFileTmp.toIO,
      scriptArgs = scriptArgs.toArray,
      verbose = replpp.verboseEnabled(config)
    ).compileAndRun() match {
      case Some(exception) =>
        System.err.println(s"error during script execution: ${exception.getMessage}")
        System.err.println(s"note: line numbers may not be accurate - to help with debugging, the final scriptContent is at $predefPlusScriptFileTmp")
        throw exception
      case None => // no exception, i.e. all is good
        System.err.println(s"script finished successfully")
        // if the script failed, we don't want to delete the temporary file which includes the predef,
        // so that the line numbers are accurate and the user can properly debug
        os.remove(predefPlusScriptFileTmp)
    }
  }

  private def wrapForMainargs(predefCode: String, scriptCode: String): String = {
    val mainImpl =
      if (scriptCode.contains("@main")) scriptCode
      else
        s"""@main def _execMain(): Unit = {
           |  $scriptCode
           |}
           |""".stripMargin

    s"""import mainargs.main // intentionally shadow any potentially given @main
       |
       |// ScriptingDriver expects an object with a predefined name and a main entrypoint method
       |object ${ScriptingDriver.MainClassName} {
       |
       |$predefCode
       |
       |$mainImpl
       |
       |  def ${ScriptingDriver.MainMethodName}(args: Array[String]): Unit = {
       |    mainargs.ParserForMethods(this).runOrExit(args.toSeq)
       |  }
       |}
       |""".stripMargin
  }

  private def wrapForMainargsFoo(predefCode: String, scriptCode: String): String = {
    s"""
       |object ${ScriptingDriver.MainClassName} {
       |
       | def ${ScriptingDriver.MainMethodName}(args: Array[String]): Unit = {
       |   $scriptCode
       |  }
       |}
       |""".stripMargin
  }

}
