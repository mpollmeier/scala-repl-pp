package stringcalc

import java.nio.file.{Files, Path}
import scopt.OParser

def main(args: Array[String]) = {
  Config.parse(args) match {
    case Some(config) =>
      replpp.Main.run(
        replpp.Config(
          scriptFile = config.scriptFile,
          prompt = Some("stringcalc"),
          greeting = Some("Welcome to the magical world of string calculation! \nType `help` for help"),
          verbose = config.verbose,
          runBefore = Seq(
            "import stringcalc.*",
            "import StringCalculator.*",
            """def help: Unit = println("try this: `add(One, Two)`")"""
          )
        )
      )
    case None => System.exit(1)
  }
}

case class Config(verbose: Boolean = false, scriptFile: Option[Path] = None)
object Config {
  def parse(args: Array[String]): Option[Config] =
    OParser.parse(parser, args, Config())

  private val builder = OParser.builder[Config]
  private val parser = {
    import builder._
    OParser.sequence(
      programName("stringcalc"),
      opt[Boolean]('v', "verbose")
        .action((x, c) => c.copy(verbose = x))
        .text("enable verbose mode"),
      opt[Path]("script")
        .action((x, c) => c.copy(scriptFile = Option(x)))
        .text("path to script file")
        .validate(path =>
          if (Files.exists(path)) success
          else failure(s"script file $path does not exist")
        )
    )
  }
}
