package replpp

import replpp.util.SimpleDriver

import java.nio.file.Path

object InteractiveShell {

  def run(config: Config): Unit = {
    import config.colors

    // precompile given predef files (if any)
    val classfiles = new SimpleDriver().compile(
      replpp.compilerArgs(config),
      inputFiles = Seq(Path.of("/home/mp/tmp/aaa.scala")),
      verbose = false
    ).get

    val compilerArgs = replpp.compilerArgs(config.withAdditionalClasspathEntries(classfiles))
    if (verboseEnabled(config))
      println(s"compiler arguments: ${compilerArgs.mkString(",")}")

    new ReplDriver(
      compilerArgs,
      onExitCode = config.onExitCode,
      greeting = config.greeting,
      prompt = config.prompt.getOrElse("scala"),
      maxHeight = config.maxHeight
    ).runUntilQuit()
  }

}