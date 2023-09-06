package replpp.scripting

import replpp.Config
import replpp.util.ClasspathHelper

import scala.util.{Failure, Success, Try}
import sys.process.Process

/** Executes a script by spawning/forking a new JVM process and then invoking the `NonForkingScriptRunner`.
  *
  * Alternatively you can directly invoke the `NonForkingScriptRunner`, but some environments have complex classloader
  * setups which cause issues with the non-forking ScriptRunner - examples include some IDEs and
  * sbt (e.g. test|console) in non-fork mode. Therefor, this forking ScriptRunner is the default one. */
object ScriptRunner {
  
  val RemoteJvmDebugConfig = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"

  def exec(config: Config): Try[Unit] = {
    val classpath = ClasspathHelper.create(config, quiet = true)
    val mainClass = "replpp.scripting.NonForkingScriptRunner"
    val args = {
      val builder = Seq.newBuilder[String]
      if (config.remoteJvmDebugEnabled) builder += RemoteJvmDebugConfig
      builder ++= Seq("-classpath", classpath)
      builder += mainClass
      builder ++= config.asJavaArgs

      builder.result()
    }
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
