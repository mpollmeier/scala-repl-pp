package replpp

import dotty.tools.Settings
import dotty.tools.io.{ClassPath, Directory, PlainDirectory}
import dotty.tools.repl.State

import java.lang.System.lineSeparator
import scala.util.control.NoStackTrace

object InteractiveShell {
  def run(config: Config): Unit = {

    val predefCode = allPredefCode(config)
    val compilerArgs = replpp.compilerArgs(config)
    import config.colors
    val replDriver = new ReplDriver(
      compilerArgs,
      onExitCode = config.onExitCode,
      greeting = config.greeting,
      prompt = config.prompt.getOrElse("scala"),
      maxHeight = config.maxHeight
    )

    val initialState: State = replDriver.initialState
    val state: State = {
      if (verboseEnabled(config)) {
        println(s"compiler arguments: ${compilerArgs.mkString(",")}")
        println(predefCode)
        replDriver.run(predefCode)(using initialState)
      } else {
        replDriver.runQuietly(predefCode)(using initialState)
      }
    }

    if (predefCode.nonEmpty && state.objectIndex != 1) {
      throw new AssertionError(s"compilation error for predef code - error should have been reported above ^") with NoStackTrace
    }

    replDriver.runUntilQuit(using state)()
  }
}
