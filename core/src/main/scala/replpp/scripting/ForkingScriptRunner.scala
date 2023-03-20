package replpp.scripting

import replpp.Config

import sys.process.Process

/** ScriptRunner: executes a script in a separate java process.
  * Note: there's also a non-forking ScriptRunner, but this one is the default, because some environments have
  * complex classloader setups that cause issues with the non-forking ScriptRunner - examples include `sbt test`,
  * `sbt console` and some certain IDEs) */
object ForkingScriptRunner {

  def exec(config: Config): Unit = {
    val args = Seq(
      "-classpath",
      replpp.classpath(config),
      "replpp.scripting.ScriptRunner",
    ) ++ config.asJavaArgs
    if (replpp.verboseEnabled(config)) println(s"executing `java ${args.mkString(" ")}`")
    val exitValue = Process("java", args).run().exitValue()
    assert(exitValue == 0, s"error while invoking `java ${args.mkString(" ")}`. exit code was $exitValue")
  }

  def main(args: Array[String]): Unit = {
    val config = Config.parse(args)
    exec(config)
  }

}
