package replpp

import dotty.tools.Settings
import dotty.tools.dotc.core.Contexts.{Context, ctx}
import dotty.tools.io.{ClassPath, Directory, PlainDirectory}
import dotty.tools.repl.State
import dotty.tools.scripting.{ScriptingDriver, Util}
import System.lineSeparator

object InteractiveShell {
  def run(config: Config): Unit = {
    val replDriver = new ReplDriver(
      compilerArgs(config),
      onExitCode = config.onExitCode,
      greeting = config.greeting,
      prompt = config.prompt.getOrElse("scala"),
      maxPrintElements = Int.MaxValue
    )

    val initialState: State = replDriver.initialState
    val predefCode = {
      val lines = readPredefFiles(config.predefFiles) :+ config.predefCode.getOrElse("")
      lines.mkString(lineSeparator)
    }
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
