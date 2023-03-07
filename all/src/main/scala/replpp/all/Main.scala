package replpp.all

import replpp.{Config, InteractiveShell}
import replpp.scripting.ForkingScriptRunner
import replpp.server.ReplServer

import java.io.{InputStream, PrintStream, File as JFile}
import java.net.URLClassLoader
import java.nio.file.{Files, Path, Paths}
import java.util
import java.util.stream
import java.util.stream.Collectors
import scala.jdk.CollectionConverters.*

object Main {
  def main(args: Array[String]): Unit = {
    val config = Config.parse(args)

    if (config.server) {
      ReplServer.startHttpServer(config)
    } else if (config.scriptFile.isDefined)
      ForkingScriptRunner.main(args)
    else 
      InteractiveShell.run(config)
  }
}
