package replpp

import java.nio.file.Path
import replpp.scripting.{NonForkingScriptRunner, ScriptRunner}

// TODO remove me
object FooRemoveMe {

  def main(args: Array[String]): Unit = {
    val scriptSrc =
      s"""val i = 2 + 10
         |println("in main/script: i=" + i)
         |val x: Int = aaa // TODO: goal1: this should come from some predef file, and the error should be reported as line number 3, not 13
         |""".stripMargin
    val scriptFile = os.temp(scriptSrc).toNIO
    val config = Config(
      scriptFile = Some(scriptFile),
      predefFiles = Seq(Path.of("/home/mp/tmp/aaa.scala")),
      verbose = false)
//    ScriptRunner.exec(config)
    NonForkingScriptRunner.exec(config)
  }

}
