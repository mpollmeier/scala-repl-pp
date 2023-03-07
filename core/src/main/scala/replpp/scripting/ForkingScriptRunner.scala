package replpp.scripting

import replpp.Config

import sys.process.Process

/** TODO describe why we have this and why this should be the default. */
object ForkingScriptRunner {

  def exec(config: Config): Unit = {
    // TODO pass on other config options
    // TODO pass on JAVA_OPTS etc. - and document how to do that

    val args = Seq(
      "-classpath",
      replpp.classpath(config),
      "replpp.scripting.ScriptRunner",
    ) ++ config.asJavaArgs
    //    if (replpp.verboseEnabled(config)) println(s"forking jvm - executing `java ${args.mkString(" ")}`")
    println(s"forking jvm: executing `java ${args.mkString(" ")}`")
//    Thread.sleep(100000)
    val p = Process("java", args).run()
    assert(p.exitValue() == 0, s"error while invoking `java ${args.mkString(" ")}`. exit code was ${p.exitValue()}")
  }

  def main(args: Array[String]): Unit = {
    val config = Config.parse(args)
    exec(config)
  }

}
