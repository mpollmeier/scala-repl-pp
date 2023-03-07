package replpp

import dotty.tools.Settings
import dotty.tools.io.{ClassPath, Directory, PlainDirectory}
import dotty.tools.repl.State

import java.lang.System.lineSeparator

object InteractiveShell {
  def run(config: Config): Unit = {

    val predefCode = allPredefCode(config)
    val compilerArgs = replpp.compilerArgs(config)
    val replDriver = new ReplDriver(
      compilerArgs,
      onExitCode = config.onExitCode,
      greeting = Option(config.greeting),
      prompt = config.prompt.getOrElse("scala"),
      maxPrintElements = Int.MaxValue
    )

    val initialState: State = replDriver.initialState
    val state: State =
      if (verboseEnabled(config)) {
        println(s"compiler arguments: ${compilerArgs.mkString(",")}")
        println(predefCode)
        replDriver.run(predefCode)(using initialState)
      } else {
        replDriver.runQuietly(predefCode)(using initialState)
      }

    replDriver.runUntilQuit(using state)()
  }
}
