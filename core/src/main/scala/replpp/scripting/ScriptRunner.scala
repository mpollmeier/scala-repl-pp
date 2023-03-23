package replpp.scripting

import replpp.Config

import scala.util.{Failure, Success, Try}
import sys.process.Process

/** ScriptRunner: executes a script in a separate java process.
  * Note: there's also a non-forking ScriptRunner, but this one is the default, because some environments have
  * complex classloader setups that cause issues with the non-forking ScriptRunner - examples include `sbt test`,
  * `sbt console` and some certain IDEs) */
object ScriptRunner {

  def exec(config: Config): Try[Unit] = {
    val args = Seq(
      "-classpath",
      replpp.classpath(config),
      "replpp.scripting.NonForkingScriptRunner",
    ) ++ config.asJavaArgs
    if (replpp.verboseEnabled(config)) println(s"executing `java ${args.mkString(" ")}`")
    Process("java", args).run().exitValue() match {
      case 0 => Success(())
      case nonZeroExitValue => 
        Failure(new AssertionError(
          s"${getClass.getName}: error while invoking `java ${args.mkString(" ")}`: exit code was $nonZeroExitValue"
        ))
    }
  }

  def main(args: Array[String]): Unit = {
    val config = Config.parse(args)
    exec(config)
  }

}
