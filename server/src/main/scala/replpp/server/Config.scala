package replpp.server

import replpp.Config.opts
import replpp.shaded.scopt.OParser

case class Config(baseConfig: replpp.Config,
                  serverHost: String = "localhost",
                  serverPort: Int = 8080,
                  serverAuthUsername: Option[String] = None,
                  serverAuthPassword: Option[String] = None)

object Config {

  def parse(args: Array[String]): Config = {
    val builder = OParser.builder[Config]
    import builder.*
    val parser = OParser.sequence(
      programName("scala-repl-pp-server"),
      replpp.Config.opts.predef(builder)((x, c) => c.copy(baseConfig = c.baseConfig.copy(predefFiles = c.baseConfig.predefFiles :+ x))),
      replpp.Config.opts.verbose(builder)((_, c) => c.copy(baseConfig = c.baseConfig.copy(verbose = true))),

      opt[String]("server-host")
        .action((x, c) => c.copy(serverHost = x))
        .text("Hostname on which to expose the REPL server"),

      opt[Int]("server-port")
        .action((x, c) => c.copy(serverPort = x))
        .text("Port on which to expose the REPL server"),

      opt[String]("server-auth-username")
        .action((x, c) => c.copy(serverAuthUsername = Option(x)))
        .text("Basic auth username for the REPL server"),

      opt[String]("server-auth-password")
        .action((x, c) => c.copy(serverAuthPassword = Option(x)))
        .text("Basic auth password for the REPL server"),

      help("help").text("Print this help text"),
    )

    OParser.parse(parser, args, Config(replpp.Config()))
      .getOrElse(throw new AssertionError("error while parsing commandline args - see errors above"))
  }
}