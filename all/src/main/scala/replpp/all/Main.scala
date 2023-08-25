package replpp.all

import replpp.scripting.ScriptRunner
import replpp.server.ReplServer
import replpp.Config

object Main {
  def main(args: Array[String]): Unit = {
    val config = Config.parse(args)

    if (config.server)
      ReplServer.startHttpServer(config)
    else 
      replpp.Main.main(args)
  }
}
