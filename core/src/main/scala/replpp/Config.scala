package replpp

import replpp.Colors.{BlackWhite, Default}
import replpp.scripting.ScriptRunner
import replpp.shaded.scopt.OParser
import replpp.shaded.scopt.OParserBuilder

import java.nio.file.Path

case class Config(
  predefFiles: Seq[Path] = Nil, // these files will be precompiled and added to the classpath
  runBefore: Seq[String] = Nil, // these statements will be interpreted on startup
  runAfter: Seq[String] = Nil,  // these statements will be interpreted on shutdown
  nocolors: Boolean = false,
  verbose: Boolean = false,
  classpathConfig: Config.ForClasspath = Config.ForClasspath(),
  remoteJvmDebugEnabled: Boolean = false,

  // repl only
  prompt: Option[String] = None,
  greeting: Option[String] = Some(defaultGreeting),
  maxHeight: Option[Int] = None,

  // script only
  scriptFile: Option[Path] = None,
  command: Option[String] = None,
  params: Map[String, String] = Map.empty,
) {
  implicit val colors: Colors =
    if (nocolors) Colors.BlackWhite
    else Colors.Default

  def withAdditionalClasspathEntry(entry: Path): Config =
    copy(classpathConfig = classpathConfig.withAdditionalClasspathEntry(entry))

  /** inverse of `Config.parse` */
  lazy val asJavaArgs: Seq[String] = {
    val args = Seq.newBuilder[String]
    def add(entries: String*) = args.addAll(entries)

    predefFiles.foreach { predefFile =>
      add("--predef", predefFile.toString)
    }

    runBefore.foreach { runBefore =>
      add("--runBefore", runBefore)
    }

    runAfter.foreach { runAfter =>
      add("--runAfter", runAfter)
    }

    if (nocolors) add("--nocolors")
    if (verbose) add("--verbose")

    classpathConfig.additionalClasspathEntries.foreach { resolver =>
      add("--classpathEntry", resolver)
    }

    if (classpathConfig.inheritClasspath) add("--cpinherit")

    classpathConfig.inheritClasspathIncludes
      .filterNot(Config.ForClasspath.DefaultInheritClasspathIncludes.contains)
      .foreach { entry =>
        add("--cpinclude", entry)
      }

    classpathConfig.inheritClasspathExcludes.foreach { entry =>
      add("--cpexclude", entry)
    }

    classpathConfig.dependencies.foreach { dependency =>
      add("--dep", dependency)
    }

    classpathConfig.resolvers.foreach { resolver =>
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
    val scalacVersion = classOf[dotty.tools.dotc.Driver].getPackage.getImplementationVersion
    val javaVersion = sys.props("java.version")
    s"Welcome to scala-repl-pp $replppVersion (Scala $scalacVersion, Java $javaVersion)"
  }

  def parse(args: Array[String]): Config = {
    OParser.parse(parser, args, Config())
      .getOrElse(throw new AssertionError("error while parsing commandline args - see errors above"))
  }

  lazy val parser = {
    given builder: OParserBuilder[Config] = OParser.builder[Config]
    import builder.*
    OParser.sequence(
      programName("scala-repl-pp"),
      opts.predef((x, c) => c.copy(predefFiles = c.predefFiles :+ x)),
      opts.runBefore((x, c) => c.copy(runBefore = c.runBefore :+ x)),
      opts.runAfter((x, c) => c.copy(runAfter = c.runAfter :+ x)),
      opts.nocolors((_, c) => c.copy(nocolors = true)),
      opts.verbose((_, c) => c.copy(verbose = true)),
      opts.classpathEntry((x, c) => c.copy(classpathConfig = c.classpathConfig.copy(additionalClasspathEntries = c.classpathConfig.additionalClasspathEntries :+ x))),
      opts.inheritClasspath((_, c) => c.copy(classpathConfig = c.classpathConfig.copy(inheritClasspath = true))),
      opts.classpathIncludesEntry((x, c) => c.copy(classpathConfig = c.classpathConfig.copy(inheritClasspathIncludes = c.classpathConfig.inheritClasspathIncludes :+ x))),
      opts.classpathExcludesEntry((x, c) => c.copy(classpathConfig = c.classpathConfig.copy(inheritClasspathExcludes = c.classpathConfig.inheritClasspathExcludes :+ x))),
      opts.dependency((x, c) => c.copy(classpathConfig = c.classpathConfig.copy(dependencies = c.classpathConfig.dependencies :+ x))),
      opts.repo((x, c) => c.copy(classpathConfig = c.classpathConfig.copy(resolvers = c.classpathConfig.resolvers :+ x))),
      opts.remoteJvmDebug((_, c) => c.copy(remoteJvmDebugEnabled = true)),

      note("REPL options"),
      opts.prompt((x, c) => c.copy(prompt = Option(x))),
      opts.greeting((x, c) => c.copy(greeting = Option(x))),
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
      builder.opt[Path]("predef")
        .valueName("myScript.sc")
        .unbounded()
        .optional()
        .action(action)
        .text("given source files will be compiled and added to classpath - this may be passed multiple times")
    }

    def runBefore[C](using builder: OParserBuilder[C])(action: Action[String, C]) = {
      builder.opt[String]("runBefore")
        .valueName("'import Int.MaxValue'")
        .unbounded()
        .optional()
        .action(action)
        .text("given code will be executed on startup - this may be passed multiple times")
    }

    def runAfter[C](using builder: OParserBuilder[C])(action: Action[String, C]) = {
      builder.opt[String]("runAfter")
        .valueName("""'println("goodbye!")'""")
        .unbounded()
        .optional()
        .action(action)
        .text("given code will be executed on shutdown - this may be passed multiple times")
    }

    def nocolors[C](using builder: OParserBuilder[C])(action: Action[Unit, C]) = {
      builder.opt[Unit]("nocolors").text("turn off colors").action(action)
    }

    def verbose[C](using builder: OParserBuilder[C])(action: Action[Unit, C]) = {
      builder.opt[Unit]("verbose")
        .action(action)
        .text("enable verbose output (predef, resolved dependency jars, ...)")
    }

    def dependency[C](using builder: OParserBuilder[C])(action: Action[String, C]) = {
      builder.opt[String]("dep")
        .valueName("com.michaelpollmeier:versionsort:1.0.7")
        .unbounded()
        .optional()
        .action(action)
        .text("add artifacts (including transitive dependencies) for given maven coordinate to classpath - may be passed multiple times")
    }

    def repo[C](using builder: OParserBuilder[C])(action: Action[String, C]) = {
      builder.opt[String]("repo")
        .valueName("https://repository.apache.org/content/groups/public/")
        .unbounded()
        .optional()
        .action(action)
        .text("additional repositories to resolve dependencies - may be passed multiple times")
    }

    def classpathEntry[C](using builder: OParserBuilder[C])(action: Action[String, C]) = {
      builder.opt[String]("classpathEntry")
        .valueName("path/to/classpath")
        .unbounded()
        .optional()
        .action(action)
        .text("additional classpath entries - may be passed multiple times")
    }

    def inheritClasspath[C](using builder: OParserBuilder[C])(action: Action[Unit, C]) = {
      builder.opt[Unit]("cpinherit").text("inherit entire classpath (excludes still applies!)").action(action)
    }

    def classpathIncludesEntry[C](using builder: OParserBuilder[C])(action: Action[String, C]) = {
      builder.opt[String]("cpinclude")
        .valueName(".*scala-repl-pp.*")
        .unbounded()
        .optional()
        .action(action)
        .text("add classpath include entry (regex) for jars inherited from parent classloader - may be passed multiple times")
    }

    def classpathExcludesEntry[C](using builder: OParserBuilder[C])(action: Action[String, C]) = {
      builder.opt[String]("cpexclude")
        .valueName(".*scala-repl-pp.*")
        .unbounded()
        .optional()
        .action(action)
        .text("add classpath exclude entry (regex) for jars inherited from parent classloader - may be passed multiple times")
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

  /** Classpath configuration: specify additional dependencies via maven coordinates and resolvers, as well as
   * configure the handling of the inherited classpath (i.e. how we handle the jars that we get from
   * `java.class.path` system property as well as the  current class loaders, recursively).
   *
   * You can either inherit the entire outer classpath via `inheritEntireClasspath == true` or specify an 'includes list'
   * of regexes for jars to keep. Additionally (in combination with both options) you can specify an 'excludes list' of jars
   * to be excluded. Note that the 'includes list' has a default list `ForClasspath.DefaultInheritClasspathIncludes`.
   *
   * Implementation note: the includes and excludes lists use `String` as the list member type because `Regex` defines
   * equality etc. differently, which breaks common case class conventions.
   */
  case class ForClasspath(additionalClasspathEntries: Seq[String] = Seq.empty,
                          inheritClasspath: Boolean = false,
                          inheritClasspathIncludes: Seq[String] = ForClasspath.DefaultInheritClasspathIncludes,
                          inheritClasspathExcludes: Seq[String] = Seq.empty,
                          dependencies: Seq[String] = Seq.empty,
                          resolvers: Seq[String] = Seq.empty) {
    def withAdditionalClasspathEntry(entry: Path): ForClasspath =
      copy(additionalClasspathEntries = additionalClasspathEntries :+ util.pathAsString(entry))
  }


  object ForClasspath {
    val DefaultInheritClasspathIncludes: Seq[String] = Seq(
      "classes",
      ".*scala-repl-pp.*",
      ".*scala3-compiler_3.*",
      ".*scala3-interfaces-.*",
      ".*scala3-library_3.*",
      ".*scala-library.*",
      ".*tasty-core_3.*",
      ".*scala-asm.*",
      ".*compiler-interface.*",

      // for replpp.util.terminalWidth
      ".*jline-terminal-.*",
      ".*net.java.dev.jna.jna.*",
    )
  }
}
