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
  greeting: Option[String] = Some(defaultGreeting),
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

  private def defaultGreeting = {
    val replppVersion = getClass.getPackage.getImplementationVersion
    val scalaVersion = scala.util.Properties.versionNumberString
    val javaVersion = sys.props("java.version")
    s"Welcome to scala-repl-pp $replppVersion (Scala $scalaVersion, Java $javaVersion)"
  }

  def parse(args: Array[String]): Config = {
    OParser.parse(parser, args, Config())
      .getOrElse(throw new AssertionError("error while parsing commandline args - see errors above"))
  }

  lazy val parser = {
    given builder: OParserBuilder[Config] = OParser.builder[Config]
    import builder._
    OParser.sequence(
      programName("scala-repl-pp"),
      opts.predef((x, c) => c.copy(predefFiles = c.predefFiles :+ x)),
      opts.nocolors((_, c) => c.copy(nocolors = true)),
      opts.verbose((_, c) => c.copy(verbose = true)),
      opts.dependency((x, c) => c.copy(dependencies = c.dependencies :+ x)),
      opts.repo((x, c) => c.copy(resolvers = c.resolvers :+ x)),
      opts.remoteJvmDebug((_, c) => c.copy(remoteJvmDebugEnabled = true)),

      note("REPL options"),
      opts.prompt((x, c) => c.copy(prompt = Option(x))),
      opts.greeting((x, c) => c.copy(greeting = Option(x))),
      opts.onExitCode((x, c) => c.copy(onExitCode = Option(x))),
      opts.maxHeight((x, c) => c.copy(maxHeight = Some(x))),

      note("Script execution"),
      opts.script((x, c) => c.copy(scriptFile = Some(x))),
      opts.command((x, c) => c.copy(command = Some(x))),
      opts.param { (x, c) =>
        x.split("=", 2) match {
          case Array(key, value) => c.copy(params = c.params + (key -> value))
          case _ => throw new IllegalArgumentException(s"unable to parse param input $x")
        }
      },
      help("help").text("Print this help text"),
    )
  }

  /** configuration arguments should be composable - they're reused in `replpp.server.Config` */
  private [replpp] object opts {
    type Action[A, C] = (A, C) => C

    def predef[C](using builder: OParserBuilder[C])(action: Action[Path, C]) = {
      builder.opt[Path]('p', "predef")
        .valueName("myScript.sc")
        .unbounded()
        .optional()
        .action(action)
        .text("import additional script files on startup - may be passed multiple times")
    }

    def nocolors[C](using builder: OParserBuilder[C])(action: Action[Unit, C]) = {
      builder.opt[Unit]("nocolors").text("turn off colors").action(action)
    }

    def verbose[C](using builder: OParserBuilder[C])(action: Action[Unit, C]) = {
      builder.opt[Unit]('v', "verbose")
        .action(action)
        .text("enable verbose output (predef, resolved dependency jars, ...)")
    }

    def dependency[C](using builder: OParserBuilder[C])(action: Action[String, C]) = {
      builder.opt[String]('d', "dep")
        .valueName("com.michaelpollmeier:versionsort:1.0.7")
        .unbounded()
        .optional()
        .action(action)
        .text("add artifacts (including transitive dependencies) for given maven coordinate to classpath - may be passed multiple times")
    }

    def repo[C](using builder: OParserBuilder[C])(action: Action[String, C]) = {
      builder.opt[String]('r', "repo")
        .valueName("https://repository.apache.org/content/groups/public/")
        .unbounded()
        .optional()
        .action(action)
        .text("additional repositories to resolve dependencies - may be passed multiple times")
    }

    def remoteJvmDebug[C](using builder: OParserBuilder[C])(action: Action[Unit, C]) = {
      builder.opt[Unit]("remoteJvmDebug")
        .action(action)
        .text(s"enable remote jvm debugging: '${ScriptRunner.RemoteJvmDebugConfig}'")
    }

    def prompt[C](using builder: OParserBuilder[C])(action: Action[String, C]) = {
      builder.opt[String]("prompt")
        .valueName("scala")
        .action(action)
        .text("specify a custom prompt")
    }

    def greeting[C](using builder: OParserBuilder[C])(action: Action[String, C]) = {
      builder.opt[String]("greeting")
        .valueName("Welcome to scala-repl-pp!")
        .action(action)
        .text("specify a custom greeting")
    }

    def onExitCode[C](using builder: OParserBuilder[C])(action: Action[String, C]) = {
      builder.opt[String]("onExitCode")
        .valueName("""println("bye!")""")
        .action(action)
        .text("code to execute when exiting")
    }

    def maxHeight[C](using builder: OParserBuilder[C])(action: Action[Int, C]) = {
      builder.opt[Int]("maxHeight")
        .action(action)
        .text("Maximum number lines to print before output gets truncated (default: no limit)")
    }

    def script[C](using builder: OParserBuilder[C])(action: Action[Path, C]) = {
      builder.opt[Path]("script")
        .action(action)
        .text("path to script file: will execute and exit")
    }

    def command[C](using builder: OParserBuilder[C])(action: Action[String, C]) = {
      builder.opt[String]("command")
        .action(action)
        .text("command to execute, in case there are multiple @main entrypoints")
    }

    def param[C](using builder: OParserBuilder[C])(action: Action[String, C]) = {
      builder.opt[String]("param")
        .valueName("param1=value1")
        .unbounded()
        .optional()
        .action(action)
        .text("key/value pair for main function in script - may be passed multiple times")
    }
  }
}