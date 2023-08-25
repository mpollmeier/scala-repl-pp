package replpp

import replpp.Colors.{BlackWhite, Default}
import replpp.scripting.ScriptRunner
import replpp.shaded.scopt.OParser
import replpp.shaded.scopt.OParserBuilder

import java.nio.file.Path

case class Config(
  predefFiles: Seq[Path] = Nil,
  nocolors: Boolean = false,
  verbose: Boolean = false,
  dependencies: Seq[String] = Seq.empty,
  resolvers: Seq[String] = Seq.empty,
  remoteJvmDebugEnabled: Boolean = false,

  // repl only
  prompt: Option[String] = None,
  greeting: String = "Welcome to scala-repl-pp!",
  onExitCode: Option[String] = None,
  maxHeight: Option[Int] = None,

  // script only
  scriptFile: Option[Path] = None,
  command: Option[String] = None,
  params: Map[String, String] = Map.empty,
) {
  implicit val colors: Colors =
    if (nocolors) Colors.BlackWhite
    else Colors.Default

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
    OParser.parse(parser, args, Config())
      .getOrElse(throw new AssertionError("error while parsing commandline args - see errors above"))
  }

  /** configuration arguments should be composable - they're reused in `replpp.server.Config` */
  object opts {
    type Action[A, C] = (A, C) => C

    def predef[C](builder: OParserBuilder[C])(action: Action[Path, C]) = {
      builder.opt[Path]('p', "predef")
        .valueName("myScript.sc")
        .unbounded()
        .optional()
        .text("import additional script files on startup - may be passed multiple times")
        .action(action)
    }

    def nocolors[C](builder: OParserBuilder[C])(action: Action[Unit, C]) =
      builder.opt[Unit]("nocolors").text("turn off colors").action(action)

    def verbose[C](builder: OParserBuilder[C])(action: Action[Unit, C]) =
      builder.opt[Unit]('v', "verbose")
        .text("enable verbose output (predef, resolved dependency jars, ...)")
        .action(action)

    def dependency[C](builder: OParserBuilder[C])(action: Action[String, C]) =
      builder.opt[String]('d', "dep")
        .valueName("com.michaelpollmeier:versionsort:1.0.7")
        .unbounded()
        .optional()
        .action(action)
        .text("add artifacts (including transitive dependencies) for given maven coordinate to classpath - may be passed multiple times")

    def repo[C](builder: OParserBuilder[C])(action: Action[String, C]) =
      builder.opt[String]('r', "repo")
        .valueName("https://repository.apache.org/content/groups/public/")
        .unbounded()
        .optional()
        .action(action)
        .text("additional repositories to resolve dependencies - may be passed multiple times")

  }

  lazy val parser = {
    val builder = OParser.builder[Config]
    import builder._
    OParser.sequence(
      programName("scala-repl-pp"),
      opts.predef(builder)((x, c) => c.copy(predefFiles = c.predefFiles :+ x)),
      opts.nocolors(builder)((_, c) => c.copy(nocolors = true)),
      opts.verbose(builder)((_, c) => c.copy(verbose = true)),
      opts.dependency(builder)((x, c) => c.copy(dependencies = c.dependencies :+ x)),
      opts.repo(builder)((x, c) => c.copy(resolvers = c.resolvers :+ x)),

      opt[Unit]("remoteJvmDebug")
        .action((_, c) => c.copy(remoteJvmDebugEnabled = true))
        .text(s"enable remote jvm debugging: '${ScriptRunner.RemoteJvmDebugConfig}'"),

      note("REPL options"),

      opt[String]("prompt")
        .valueName("scala")
        .action((x, c) => c.copy(prompt = Option(x)))
        .text("specify a custom prompt"),

      opt[String]("greeting")
        .valueName("Welcome to scala-repl-pp!")
        .action((x, c) => c.copy(greeting = x))
        .text("specify a custom greeting"),

      opt[String]("onExitCode")
        .valueName("""println("bye!")""")
        .action((x, c) => c.copy(onExitCode = Option(x))),

      opt[Int]("maxHeight")
        .action((x, c) => c.copy(maxHeight = Some(x)))
        .text("Maximum number lines to print before output gets truncated (default: no limit)"),

      note("Script execution"),

      opt[Path]("script")
        .action((x, c) => c.copy(scriptFile = Some(x)))
        .text("path to script file: will execute and exit"),

      opt[String]("command")
        .action((x, c) => c.copy(command = Some(x)))
        .text("command to execute, in case there are multiple @main entrypoints"),

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
        .text("key/value pair for main function in script - may be passed multiple times"),

      help("help").text("Print this help text"),
    )
  }
}
