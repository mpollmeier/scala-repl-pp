package replpp

import dotty.tools.repl.State

import java.lang.System.lineSeparator
import scala.util.control.NoStackTrace

object InteractiveShell {

  def run(config: Config): Unit = {
    import config.colors
    val config0 = precompilePredefFiles(config)

    val compilerArgs = replpp.compilerArgs(config0)

    val replDriver = new ReplDriver(
      compilerArgs,
      onExitCode = config0.onExitCode,
      greeting = config0.greeting,
      prompt = config0.prompt.getOrElse("scala"),
      maxHeight = config0.maxHeight
    )

    val initialState: State = replDriver.initialState
    val runBeforeLines = (DefaultRunBeforeLines ++ config.runBefore).mkString(lineSeparator)
    val state: State = {
      if (verboseEnabled(config)) {
        println(s"compiler arguments: ${compilerArgs.mkString(",")}")
        println(runBeforeLines)
        replDriver.run(runBeforeLines)(using initialState)
      } else {
        replDriver.runQuietly(runBeforeLines)(using initialState)
      }
    }

    if (runBeforeLines.nonEmpty && state.objectIndex != 1) {
      throw new AssertionError(s"compilation error for predef code - error should have been reported above ^") with NoStackTrace
    }

    replDriver.runUntilQuit(using state)()
  }
  
}
