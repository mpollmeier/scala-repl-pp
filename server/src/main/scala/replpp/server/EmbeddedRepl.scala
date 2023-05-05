package replpp.server

import dotty.tools.dotc.config.Printers.config
import dotty.tools.repl.State
import org.slf4j.{Logger, LoggerFactory}
import replpp.{Config, ReplDriverBase, allPredefLines, pwd}

import java.io.*
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.{BlockingQueue, Executors, LinkedBlockingQueue, Semaphore}
import scala.concurrent.impl.Promise
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success}

class EmbeddedRepl(config: Config) {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  /** repl and compiler output ends up in this replOutputStream */
  private val replOutputStream = new ByteArrayOutputStream()

  private val replDriver: ReplDriver = {
    val inheritedClasspath = System.getProperty("java.class.path")
    val compilerArgs = Array(
      "-classpath", inheritedClasspath,
      "-explain", // verbose scalac error messages
      "-deprecation",
      "-color", "never"
    )
    new ReplDriver(compilerArgs, new PrintStream(replOutputStream), classLoader = None)
  }

  private var state: State = {
    val state = replDriver.execute(allPredefLines(config))(using replDriver.initialState)
    val output = readAndResetReplOutputStream()
    if (output.nonEmpty)
      logger.info(output)
    state
  }

  private val singleThreadedJobExecutor: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  /** Execute `inputLines` in REPL (in single threaded ExecutorService) and provide Future for result callback */
  def queryAsync(inputLines: IterableOnce[String]): (UUID, Future[String]) = {
    val uuid = UUID.randomUUID()
    val future = Future {
      state = replDriver.execute(inputLines)(using state)
      readAndResetReplOutputStream()
    } (using singleThreadedJobExecutor)

    (uuid, future)
  }

  private def readAndResetReplOutputStream(): String = {
    val result = replOutputStream.toString(StandardCharsets.UTF_8)
    replOutputStream.reset()
    result
  }

  /** Submit query `q` to the shell and return result.
    */
  def query(inputLines: IterableOnce[String]): QueryResult = {
//    val mutex               = new Semaphore(0)
//    var result: QueryResult = null
//    queryAsync(q) { r =>
//      result = r
//      mutex.release()
//    }
//    mutex.acquire()
//    result
    ???
  }

  /** Shutdown the embedded shell and associated threads.
    */
  def shutdown(): Unit = {
    logger.info("shutting down")
    singleThreadedJobExecutor.shutdown()
  }
}

class ReplDriver(args: Array[String], out: PrintStream, classLoader: Option[ClassLoader])
  extends ReplDriverBase(args, out, classLoader) {
  def execute(inputLines: IterableOnce[String])(using state: State = initialState): State =
    interpretInput(inputLines, state, pwd)
}
