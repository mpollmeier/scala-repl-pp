package replpp

import replpp.scripting.ScriptRunner

object Main {
  def main(args: Array[String]): Unit = {
    val config = Config.parse(args)

    if (config.scriptFile.isDefined) {
      ScriptRunner.main(args)
    } else {
      InteractiveShell.run(config)
    }
  }
}
