package replpp.server

import replpp.Config

// TODO factor out server mode into separate subproject
object Server {
  def start(config: Config): Unit = {
    ???
  }
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

}
