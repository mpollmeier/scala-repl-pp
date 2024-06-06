package replpp.scripting

import replpp.{Config, allPredefFiles}

import java.nio.file.Files

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

    val verboseEnabled = replpp.verboseEnabled(config)
    new ScriptingDriver(
      compilerArgs = replpp.compilerArgs(config) :+ "-nowarn",
      predefFiles = allPredefFiles(config),
      scriptFile = scriptFile,
      scriptArgs = scriptArgs.toArray,
      verbose = verboseEnabled
    ).compileAndRun() match {
      case Some(exception) =>
        System.err.println(s"error during script execution: ${exception.getMessage}")
        throw exception
      case None => // no exception, i.e. all is good
        if (verboseEnabled) System.err.println(s"script finished successfully")
    }
  }


}
