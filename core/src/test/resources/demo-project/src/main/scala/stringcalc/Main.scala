package stringcalc

import java.nio.file.Files
import replpp.{Config, InteractiveShell}

object Main {
  @main def startRepl(): Unit = {
    val predefFileTmp = Files.createTempFile("scala-repl-pp-demo-project-predef", ".sc")
    Files.writeString(
      predefFileTmp,
      s"""import stringcalc._
         |import StringCalculator._
         |
         |def help: Unit = println("try this: `add(One, Two)`")
      """.stripMargin
    )

    InteractiveShell.run(
      Config(
        prompt = Some("stringcalc"),
        greeting = "Welcome to the magical world of string calculation! \nType `help` for help",
        predefFiles = Seq(predefFileTmp),
      )
    )
  }
}
