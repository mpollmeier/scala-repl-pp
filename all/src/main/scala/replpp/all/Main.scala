package replpp.all

import replpp.scripting.ScriptRunner
import replpp.server.ReplServer
import replpp.{Config, InteractiveShell}

object Main {
  def main(args: Array[String]): Unit = {
    val config = Config.parse(args)

    if (config.server) {
      ReplServer.startHttpServer(config)
    } else if (config.scriptFile.isDefined)
      ScriptRunner.main(args)
    else 
      InteractiveShell.run(config)
  }
}
