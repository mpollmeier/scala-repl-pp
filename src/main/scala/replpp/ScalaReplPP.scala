package replpp

import os.{Path, pwd}
import scala.jdk.CollectionConverters._

import java.io.{InputStream, PrintStream, File as JFile}
import java.net.URLClassLoader
import java.nio.file.{Files, Path, Paths}
import java.util
import java.util.stream
import java.util.stream.Collectors

object ScalaReplPP {
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


//   private def maybeAddDependencies(scriptCode: String, config: Config): Config = {
//     val usingClausePrefix = "//> using "
//     val dependenciesFromUsingClauses =
//       scriptCode.lines()
//         .map(_.trim)
//         .filter(_.startsWith(usingClausePrefix))
//         .map(_.drop(usingClausePrefix.length))
//         .collect(Collectors.toList)
//         .asScala

//     config.copy(dependencies = config.dependencies ++ dependenciesFromUsingClauses)
//   }

//   /** For the given config, generate a list of commands to import the CPG
//     */
//   private def importCpgCode(config: Config): List[String] = {
//     config.cpgToLoad.map { cpgFile =>
//       "importCpg(\"" + cpgFile + "\")"
//     }.toList ++ config.forInputPath.map { name =>
//       s"""
//          |openForInputPath(\"$name\")
//          |""".stripMargin
//     }
//   }



// TODO factor out server mode into separate subproject
// trait ServerHandling { this: BridgeBase =>

//   protected def startHttpServer(config: Config): Unit = {
//     val predef   = predefPlus(additionalImportCode(config))
//     val ammonite = new EmbeddedAmmonite(predef, config.verbose)
//     ammonite.start()
//     Runtime.getRuntime.addShutdownHook(new Thread(() => {
//       println("Shutting down CPGQL server...")
//       ammonite.shutdown()
//     }))
//     val server = new CPGQLServer(
//       ammonite,
//       config.serverHost,
//       config.serverPort,
//       config.serverAuthUsername,
//       config.serverAuthPassword
//     )
//     println("Starting CPGQL server ...")
//     try {
//       server.main(Array.empty)
//     } catch {
//       case _: java.net.BindException =>
//         println("Could not bind socket for CPGQL server, exiting.")
//         ammonite.shutdown()
//         System.exit(1)
//       case e: Throwable =>
//         println("Unhandled exception thrown while attempting to start CPGQL server: ")
//         println(e.getMessage)
//         println("Exiting.")

//         ammonite.shutdown()
//         System.exit(1)
//     }
//   }

// }
