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

    if (verbose) println(s"compiler arguments: ${compilerArgs.mkString(",")}")

    var state: State = replDriver.initialState
    var expectedStateObjectIndex = 0
    Seq(DefaultRunBeforeLines, globalRunBeforeLines, config.runBefore).foreach { runBeforeLines =>
      val runBeforeCode = runBeforeLines.mkString("\n").trim
      if (runBeforeCode.nonEmpty) {
        expectedStateObjectIndex += 1
        state =
          if (verbose) {
            println(s"executing runBeforeCode: $runBeforeCode")
            replDriver.run(runBeforeCode)(using state)
          } else {
            replDriver.runQuietly(runBeforeCode)(using state)
          }
      }
    }

    assert(
      state.objectIndex == expectedStateObjectIndex,
      s"compilation error(s) for predef code - see error above ^^^"
    )

    replDriver.runUntilQuit(using state)()
  }
  
}
