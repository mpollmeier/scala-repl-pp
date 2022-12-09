package replpp

import dotty.tools.Settings
import dotty.tools.io.{ClassPath, Directory, PlainDirectory}
import dotty.tools.repl.State

import java.lang.System.lineSeparator

object InteractiveShell {
  def run(config: Config): Unit = {
    val predefCode = allPredefCode(config)
    val replDriver = new ReplDriver(
      compilerArgs(config, predefCode),
      onExitCode = config.onExitCode,
      greeting = Option(config.greeting),
      prompt = config.prompt.getOrElse("scala"),
      maxPrintElements = Int.MaxValue
    )

    val initialState: State = replDriver.initialState
    val state: State =
      if (config.verbose) {
        println(predefCode)
        replDriver.run(predefCode)(using initialState)
      } else {
        replDriver.runQuietly(predefCode)(using initialState)
      }

    replDriver.runUntilQuit(using state)()
  }
}
