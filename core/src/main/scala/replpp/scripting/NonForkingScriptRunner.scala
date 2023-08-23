package replpp.scripting

import java.util.stream.Collectors
import replpp.Config
import replpp.allPredefCode

import java.nio.file.Files
import scala.collection.immutable.{AbstractSeq, LinearSeq}
import scala.jdk.CollectionConverters.*

/**
  * Main entrypoint for ScriptingDriver, i.e. it takes commandline arguments and executes a script on the current JVM. 
  * Note: because it doesn't spawn a new JVM it doesn't work in all setups: e.g. when starting from `sbt test` 
  * with `fork := false` it runs into classloader/classpath issues. Same goes for some IDEs, depending on their 
  * classloader setup. 
  * Because of these issues, the forking `ScriptRunner` is the default option. It simply spawns a new JVM and invokes 
  * the NonForkingScriptRunner :)
  */
object NonForkingScriptRunner {

  def main(args: Array[String]): Unit = {
    val config = Config.parse(args)
    exec(config)
  }

  def exec(config: Config): Unit = {
    val scriptFile = config.scriptFile.getOrElse(throw new AssertionError("script file not defined - please specify e.g. via `--script=myscript.sc`"))
    if (!Files.exists(scriptFile)) {
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
    val scriptCode = Files.readString(scriptFile)
    val scriptContent = wrapForMainargs(predefCode, scriptCode)
    val predefPlusScriptFileTmp = Files.createTempFile("scala-repl-pp-script-with-predef", ".sc")
    Files.writeString(predefPlusScriptFileTmp, scriptContent)

    val compilerArgs = replpp.compilerArgs(config) :+ "-nowarn"
    val verboseEnabled = replpp.verboseEnabled(config)
    new ScriptingDriver(
      compilerArgs = compilerArgs,
      scriptFile = predefPlusScriptFileTmp,
      scriptArgs = scriptArgs.toArray,
      verbose = verboseEnabled
    ).compileAndRun() match {
      case Some(exception) =>
        System.err.println(s"error during script execution: ${exception.getMessage}")
        System.err.println(s"note: line numbers may not be accurate - to help with debugging, the final scriptContent is at $predefPlusScriptFileTmp")
        throw exception
      case None => // no exception, i.e. all is good
        if (verboseEnabled) System.err.println(s"script finished successfully")
        // if the script failed, we don't want to delete the temporary file which includes the predef,
        // so that the line numbers are accurate and the user can properly debug
        Files.deleteIfExists(predefPlusScriptFileTmp)
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

    s"""import replpp.shaded.mainargs
       |import mainargs.main // intentionally shadow any potentially given @main
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

}
