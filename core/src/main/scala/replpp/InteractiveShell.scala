package replpp

import dotty.tools.repl.State

import scala.util.control.NoStackTrace

object InteractiveShell {

  def run(config: Config): Unit = {
    import config.colors
    val config0 = precompilePredefFiles(config)
    val compilerArgs = replpp.compilerArgs(config0)
    val verbose = verboseEnabled(config)

    val replDriver = new ReplDriver(
      compilerArgs,
      greeting = config0.greeting,
      prompt = config0.prompt.getOrElse("scala"),
      maxHeight = config0.maxHeight,
      runAfter = config0.runAfter,
      verbose = verbose
    )

    val initialState: State = replDriver.initialState
    val runBeforeCode = (DefaultRunBeforeLines ++ globalRunBeforeLines ++ config.runBefore).mkString("\n")
    val state: State = {
      if (verbose) {
        println(s"compiler arguments: ${compilerArgs.mkString(",")}")
        println(runBeforeCode)
        replDriver.run(runBeforeCode)(using initialState)
      } else {
        replDriver.runQuietly(runBeforeCode)(using initialState)
      }
    }

    if (runBeforeCode.nonEmpty && state.objectIndex != 1) {
      throw new RuntimeException(s"compilation error for predef code - error should have been reported above ^^^")
    }

    replDriver.runUntilQuit(using state)()
  }
  
}
