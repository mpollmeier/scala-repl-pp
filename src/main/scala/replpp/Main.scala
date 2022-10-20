package replpp

import os.{Path, pwd}
import scala.jdk.CollectionConverters._

import java.io.{InputStream, PrintStream, File as JFile}
import java.net.URLClassLoader
import java.nio.file.{Files, Path, Paths}
import java.util
import java.util.stream
import java.util.stream.Collectors

object Main {
  def main(args: Array[String]): Unit = {
    val config = Config.parse(args)
    config.scriptFile match {
      case Some(scriptFile) => 
        ScriptRunner.exec(config)
      case None => 
        InteractiveShell.run(config)
    }
  }
}
