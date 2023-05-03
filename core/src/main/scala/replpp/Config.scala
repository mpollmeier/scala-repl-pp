package replpp

import java.nio.file.Path

case class Config(
  predefFiles: Seq[Path] = Nil,
  nocolors: Boolean = false,
  verbose: Boolean = false,
  dependencies: Seq[String] = Seq.empty,
  resolvers: Seq[String] = Seq.empty,

  // repl only
  prompt: Option[String] = None,
  greeting: String = "Welcome to scala-repl-pp!",
  onExitCode: Option[String] = None,
  maxHeight: Option[Int] = None,

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

    predefFiles.foreach { predefFile =>
      add("--predef", predefFile.toString)
    }

    if (nocolors) add("--nocolors")
    if (verbose) add("--verbose")

    dependencies.foreach { dependency =>
      add("--dep", dependency)
    }

    resolvers.foreach { resolver =>
      add("--repo", resolver)
    }

    maxHeight.foreach { value =>
      add("--maxHeight", s"$value")
    }

    scriptFile.foreach(file => add("--script", file.toString))
    command.foreach(cmd => add("--command", cmd))

    params.foreach { case (key, value) =>
      add("--param", s"$key=$value")
    }

    args.result()
  }
}

object Config {
  
  def parse(args: Array[String]): Config = {
    val parser = new scopt.OptionParser[Config](getClass.getSimpleName) {
      override def errorOnUnknownArgument = false

      opt[Path]('p', "predef")
        .valueName("myScript.sc")
        .unbounded()
        .optional()
        .action((x, c) => c.copy(predefFiles = c.predefFiles :+ x))
        .text("import additional script files on startup - may be passed multiple times")

      opt[Unit]("nocolors")
        .action((_, c) => c.copy(nocolors = true))
        .text("turn off colors")

      opt[Unit]('v', "verbose")
        .action((_, c) => c.copy(verbose = true))
        .text("enable verbose output (predef, resolved dependency jars, ...)")

      opt[String]('d', "dep")
        .valueName("com.michaelpollmeier:versionsort:1.0.7")
        .unbounded()
        .optional()
        .action((x, c) => c.copy(dependencies = c.dependencies :+ x))
        .text("add artifacts (including transitive dependencies) for given maven coordinate to classpath - may be passed multiple times")

      opt[String]('r', "repo")
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
        .valueName("Welcome to scala-repl-pp!")
        .action((x, c) => c.copy(greeting = x))
        .text("specify a custom greeting")

      opt[String]("onExitCode")
        .valueName("""println("bye!")""")
        .action((x, c) => c.copy(onExitCode = Option(x)))

      opt[Int]("maxHeight")
        .action((x, c) => c.copy(maxHeight = Some(x)))
        .text("Maximum number lines to print before output gets truncated (default: no limit)")

      note("Script execution")

      opt[Path]("script")
        .action((x, c) => c.copy(scriptFile = Some(x)))
        .text("path to script file: will execute and exit")

      opt[String]("command")
        .action((x, c) => c.copy(command = Some(x)))
        .text("command to execute, in case there are multiple @main entrypoints")

      opt[String]("param")
        .valueName("param1=value1")
        .unbounded()
        .optional()
        .action { (x, c) =>
          x.split("=", 2) match {
            case Array(key, value) => c.copy(params = c.params + (key -> value))
            case _ => throw new IllegalArgumentException(s"unable to parse param input $x")
          }
        }
        .text("key/value pair for main function in script - may be passed multiple times")

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
