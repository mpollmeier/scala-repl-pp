package replpp

import os.{Path, pwd}

// TODO split into repl|script|server config - with some options shared...
case class Config(
  // repl only
  prompt: Option[String] = None,
  greeting: Option[String] = None,
  onExitCode: Option[String] = None,

  // script only
  scriptFile: Option[os.Path] = None, // script only
  params: Map[String, String] = Map.empty, // script only

  // shared
  predefCode: Option[String] = None,
  predefFiles: List[os.Path] = Nil,
  nocolors: Boolean = false,
  verbose: Boolean = false,
  dependencies: Seq[String] = Seq.empty,

  // server only
  server: Boolean = false,
  serverHost: String = "localhost",
  serverPort: Int = 8080,
  serverAuthUsername: String = "",
  serverAuthPassword: String = "",
)

object Config {
  def parse(args: Array[String]): Config = {
    implicit def pathRead: scopt.Read[os.Path] =
      scopt.Read.stringRead.map(os.Path(_, os.pwd)) // support both relative and absolute paths

    val parser = new scopt.OptionParser[Config](getClass.getSimpleName) {
      override def errorOnUnknownArgument = false

      opt[Seq[String]]("dependency")
        .valueName("com.michaelpollmeier:versionsort:1.0.7,...")
        .action((x, c) => c.copy(dependencies = x.toList))
        .text("resolve dependency (and it's transitive dependencies) for given maven coordinate(s): comma-separated list. use `--verbose` to print resolved jars")

      opt[Seq[os.Path]]("predefFiles")
        .valueName("script1.sc,script2.sc,...")
        .action((x, c) => c.copy(predefFiles = x.toList))
        .text("import (and run) additional script(s) on startup")

      note("Script execution")

      opt[os.Path]("script")
        .action((x, c) => c.copy(scriptFile = Some(x)))
        .text("path to script file: will execute and exit")

      opt[Map[String, String]]('p', "params")
        .valueName("k1=v1,k2=v2")
        .action((x, c) => c.copy(params = x))
        .text("parameter values for main function in script")

      note("REST server mode")

      opt[Unit]("server")
        .action((_, c) => c.copy(server = true))
        .text("run as HTTP server")

      opt[String]("server-host")
        .action((x, c) => c.copy(serverHost = x))
        .text("Hostname on which to expose the REPL server")

      opt[Int]("server-port")
        .action((x, c) => c.copy(serverPort = x))
        .text("Port on which to expose the REPL server")

      opt[String]("server-auth-username")
        .action((x, c) => c.copy(serverAuthUsername = x))
        .text("Basic auth username for the REPL server")

      opt[String]("server-auth-password")
        .action((x, c) => c.copy(serverAuthPassword = x))
        .text("Basic auth password for the REPL server")

      note("Misc")

      opt[Unit]("nocolors")
        .action((_, c) => c.copy(nocolors = true))
        .text("turn off colors")

      opt[Unit]("verbose")
        .action((_, c) => c.copy(verbose = true))
        .text("enable verbose output (predef, resolved dependency jars, ...)")

      help("help")
        .text("Print this help text")
    }

    // note: if config is really `None` an error message would have been displayed earlier
    parser.parse(args, Config()).get
  }
}
