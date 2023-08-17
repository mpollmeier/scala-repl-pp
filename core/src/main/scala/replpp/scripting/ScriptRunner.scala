package replpp.scripting

import replpp.Config

import scala.util.{Failure, Success, Try}
import sys.process.Process

/** Executes a script by spawning/forking a new JVM process and then invoking the `NonForkingScriptRunner`.
  *
  * Alternatively you can directly invoke the `NonForkingScriptRunner`, but some environments have complex classloader
  * setups which cause issues with the non-forking ScriptRunner - examples include some IDEs and
  * sbt (e.g. test|console) in non-fork mode. Therefor, this forking ScriptRunner is the default one. */
object ScriptRunner {

  def exec(config: Config): Try[Unit] = {
    val classpath = replpp.classpath(config, quiet = true)
    val mainClass = "replpp.scripting.NonForkingScriptRunner"
    val args = Seq("-classpath", classpath, mainClass) ++ config.asJavaArgs
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
