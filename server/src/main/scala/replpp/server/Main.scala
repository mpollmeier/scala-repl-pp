package replpp.server

object Main {
  def main(args: Array[String]): Unit = {
    val config = Config.parse(args)
    ReplServer.startHttpServer(config)
  }
}
