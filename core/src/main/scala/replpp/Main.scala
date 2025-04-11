package replpp

import replpp.scripting.ScriptRunner

object Main {
  def main(args: Array[String]): Unit =
    run(Config.parse(args))

  def run(config: Config): Unit = {
    if (config.scriptFile.isDefined) {
      ScriptRunner.exec(config).get
    } else {
      InteractiveShell.run(config)
    }
  }
}
