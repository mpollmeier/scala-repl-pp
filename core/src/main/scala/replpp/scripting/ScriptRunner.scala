package replpp.scripting

import replpp.{Colors, Config, DefaultRunBeforeLines, globalRunBeforeLines, verboseEnabled}
import replpp.util.ClasspathHelper

import java.lang.management.ManagementFactory
import java.nio.file.Paths
import scala.jdk.CollectionConverters.*
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
    val defaultRunBeforeLines = DefaultRunBeforeLines(using Colors.BlackWhite) ++ globalRunBeforeLines
    val config0 = config.copy(runBefore = defaultRunBeforeLines ++ config.runBefore)
    val args = {
      val builder = Seq.newBuilder[String]
      builder ++= inheritedJvmOptions(ManagementFactory.getRuntimeMXBean.getInputArguments.asScala.toSeq)
      if (config.remoteJvmDebugEnabled) builder += RemoteJvmDebugConfig
      builder ++= Seq("-classpath", classpath)
      builder += mainClass
      builder ++= config0.asJavaArgs

      builder.result()
    }
    // the same Java as this JVM, not whichever `java` is first on the PATH
    val java = Paths.get(System.getProperty("java.home"), "bin", "java").toString
    if (replpp.verboseEnabled(config0))
      println(s"executing `$java ${args.mkString(" ")}`")

    Process(java, args).run().exitValue() match {
      case 0 => Success(())
      case nonZeroExitValue =>
        Failure(new AssertionError(
          s"${getClass.getName}: error while invoking $mainClass: exit code was $nonZeroExitValue"
        ))
    }
  }

  /** The options the script JVM takes from this JVM: heap, GC, system properties and the like. Not the ones that claim
    * a fixed port or file, which this JVM already holds, so that the script JVM would fail to start or overwrite it:
    * agents (the debugger, profilers, `-javaagent` exporters), JMX remote management and a JFR recording. */
  private[scripting] def inheritedJvmOptions(options: Seq[String]): Seq[String] =
    options.filterNot(option => NotInheritedPrefixes.exists(option.startsWith))

  private val NotInheritedPrefixes =
    Seq("-agentlib:", "-agentpath:", "-javaagent:", "-Xrunjdwp", "-Dcom.sun.management.", "-XX:StartFlightRecording")

  def main(args: Array[String]): Unit = {
    val config = Config.parse(args)
    exec(config).get
  }

}
