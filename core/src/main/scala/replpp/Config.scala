package replpp

import java.nio.file.Path

// TODO split into repl|script|server config - with some options shared...
case class Config(
  predefCode: Option[String] = None,
  predefFiles: Seq[Path] = Nil,
  predefFilesBeforePredefCode: Boolean = false,
  nocolors: Boolean = false,
  verbose: Boolean = false,
  dependencies: Seq[String] = Seq.empty,
  resolvers: Seq[String] = Seq.empty,

  // repl only
  prompt: Option[String] = None,
  greeting: String = "Welcome to the scala-repl-pp!",
  onExitCode: Option[String] = None,

  // script only
  scriptFile: Option[Path] = None,
  command: Option[String] = None,
  params: Map[String, String] = Map.empty,

  // server only
  server: Boolean = false,
  serverHost: String = "localhost",
  serverPort: Int = 8080,
  serverAuthUsername: String = "",
  serverAuthPassword: String = "",
) {

  /** inverse of `Config.parse` */
  lazy val asJavaArgs: Seq[String] = {
    val args = Seq.newBuilder[String]
    def add(entries: String*) = args.addAll(entries)

    predefCode.foreach { code =>
      // TODO maybe better to write to some file? at least for debugging this? attention: preserve same order as normally...
      add("--predefCode", code)
    }

    predefFiles.foreach { predefFile =>
      add("--predefFiles", predefFile.toString)
    }

    if (nocolors) add("--nocolors")
    if (verbose) add("--verbose")

    dependencies.foreach { dependency =>
      add("--dependencies", dependency)
    }

    resolvers.foreach { resolver =>
      add("--resolvers", resolver)
    }

    scriptFile.foreach(file => add("--script", file.toString))
    command.foreach(cmd => add("--command", cmd))

    if (params.nonEmpty) {
      add("--params",
        // ("k1=v1,k2=v2")
        params.map { (key, value) => s"$key=$value" }.mkString(",")
      )
    }

    args.result()
  }
}

object Config {
  
  def parse(args: Array[String]): Config = {
    val parser = new scopt.OptionParser[Config](getClass.getSimpleName) {
      override def errorOnUnknownArgument = false

      opt[String]("predefCode")
        .valueName("def foo = 42")
        .action((x, c) => c.copy(predefCode = Option(x)))
        .text("code to execute (quietly) on startup")

      opt[Path]("predefFiles")
        .valueName("script1.sc,script2.sc,...")
        .unbounded()
        .optional()
        .action((x, c) => c.copy(predefFiles = c.predefFiles :+ x))
        .text("import (and run) additional script(s) on startup - may be passed multiple times")

      opt[Unit]("nocolors")
        .action((_, c) => c.copy(nocolors = true))
        .text("turn off colors")

      opt[Unit]("verbose")
        .action((_, c) => c.copy(verbose = true))
        .text("enable verbose output (predef, resolved dependency jars, ...)")

      opt[String]("dependencies")
        .valueName("com.michaelpollmeier:versionsort:1.0.7")
        .unbounded()
        .optional()
        .action((x, c) => c.copy(dependencies = c.dependencies :+ x))
        .text("resolve dependencies (including transitive dependencies) for given maven coordinate(s) - may be passed multiple times")

      opt[String]("resolvers")
        .valueName("https://repository.apache.org/content/groups/public/")
        .unbounded()
        .optional()
        .action((x, c) => c.copy(resolvers = c.resolvers :+ x))
        .text("additional repositories to resolve dependencies - may be passed multiple times")

      note("REPL options")

      opt[String]("prompt")
        .valueName("scala")
        .action((x, c) => c.copy(prompt = Option(x)))
        .text("specify a custom prompt")

      opt[String]("greeting")
        .valueName("Welcome to the scala-repl-pp!")
        .action((x, c) => c.copy(greeting = x))
        .text("specify a custom greeting")

      opt[String]("onExitCode")
        .valueName("""println("bye!")""")
        .action((x, c) => c.copy(onExitCode = Option(x)))

      note("Script execution")

      opt[Path]("script")
        .action((x, c) => c.copy(scriptFile = Some(x)))
        .text("path to script file: will execute and exit")

      opt[String]("command")
        .action((x, c) => c.copy(command = Some(x)))
        .text("command to execute, in case there are multiple @main entrypoints")

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

      help("help")
        .text("Print this help text")
    }

    // note: if config is really `None` an error message would have been displayed earlier
    parser.parse(args, Config()).get
  }
}
