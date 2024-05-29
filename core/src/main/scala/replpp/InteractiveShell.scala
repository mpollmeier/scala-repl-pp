package replpp

import replpp.util.SimpleDriver

object InteractiveShell {

  def run(config: Config): Unit = {
    import config.colors

    val config0 = {
      if (config.predefFiles.nonEmpty) {
        val predefClassfiles = new SimpleDriver().compile(
          replpp.compilerArgs(config),
          inputFiles = config.predefFiles,
          verbose = config.verbose
        ).get
        config.withAdditionalClasspathEntries(predefClassfiles)
      } else config
    }

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