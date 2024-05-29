package replpp

object InteractiveShell {

  def run(config: Config): Unit = {
    import config.colors
    val config0 = precompilePredefFiles(config)

    val compilerArgs = replpp.compilerArgs(config0)
    if (verboseEnabled(config0))
      println(s"compiler arguments: ${compilerArgs.mkString(",")}")

    new ReplDriver(
      compilerArgs,
      onExitCode = config0.onExitCode,
      greeting = config0.greeting,
      prompt = config0.prompt.getOrElse("scala"),
      maxHeight = config0.maxHeight
    ).runUntilQuit()
  }
  
}