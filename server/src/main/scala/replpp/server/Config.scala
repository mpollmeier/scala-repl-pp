package replpp.server

import replpp.shaded.scopt.{OParser, OParserBuilder}

case class Config(baseConfig: replpp.Config,
                  serverHost: String = "localhost",
                  serverPort: Int = 8080,
                  serverAuthUsername: Option[String] = None,
                  serverAuthPassword: Option[String] = None)

object Config {

  def parse(args: Array[String]): Config = {
    given builder: OParserBuilder[Config] = OParser.builder[Config]
    import builder.*
    val parser = OParser.sequence(
      programName("scala-repl-pp-server"),
      replpp.Config.opts.predef((x, c) => c.copy(baseConfig = c.baseConfig.copy(predefFiles = c.baseConfig.predefFiles :+ x))),
      replpp.Config.opts.verbose((_, c) => c.copy(baseConfig = c.baseConfig.copy(verbose = true))),
      replpp.Config.opts.inheritClasspath((_, c) => c.copy(baseConfig = c.baseConfig.copy(classpathConfig = c.baseConfig.classpathConfig.copy(inheritClasspath = true)))),
      replpp.Config.opts.classpathWhitelistEntry((x, c) => {
        val bc = c.baseConfig
        val cpc = bc.classpathConfig
        c.copy(baseConfig = bc.copy(classpathConfig = cpc.copy(inheritClasspathWhitelist = cpc.inheritClasspathWhitelist :+ x)))
      }),
      replpp.Config.opts.classpathBlacklistEntry((x, c) => {
        val bc = c.baseConfig
        val cpc = bc.classpathConfig
        c.copy(baseConfig = bc.copy(classpathConfig = cpc.copy(inheritClasspathBlacklist = cpc.inheritClasspathBlacklist :+ x)))
      }),
      replpp.Config.opts.dependency((x, c) => c.copy(baseConfig = c.baseConfig.copy(classpathConfig = c.baseConfig.classpathConfig.copy(dependencies = c.baseConfig.classpathConfig.dependencies :+ x)))),
      replpp.Config.opts.repo((x, c) => c.copy(baseConfig = c.baseConfig.copy(classpathConfig = c.baseConfig.classpathConfig.copy(resolvers = c.baseConfig.classpathConfig.resolvers :+ x)))),
      replpp.Config.opts.remoteJvmDebug((_, c) => c.copy(baseConfig = c.baseConfig.copy(remoteJvmDebugEnabled = true))),

      note("Server mode"),
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