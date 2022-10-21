package stringcalc

import replpp.{Config, InteractiveShell}

object Main {
  @main def startRepl(): Unit = {
    val predef =
      s"""import stringcalc._
         |import StringCalculator._
         |
         |def help: Unit = println("try this: `add(One, Two)`")
      """.stripMargin
    InteractiveShell.run(
      Config(
        prompt = Some("stringcalc"),
        greeting = "Welcome to the magical world of string calculation! \nType `help` for help",
        predefCode = Some(predef),
      )
    )
  }
}
