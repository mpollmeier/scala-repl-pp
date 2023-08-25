package replpp

import replpp.scripting.ScriptRunner

object Main {
  def main(args: Array[String]): Unit = {
    val config = Config.parse(args)

    if (config.server) {
      System.err.println("please use `replpp.all.Main` main method to start the server")
      System.exit(1)
    } else if (config.scriptFile.isDefined) {
      ScriptRunner.main(args)
    } else {
      InteractiveShell.run(config)
    }
  }
}
