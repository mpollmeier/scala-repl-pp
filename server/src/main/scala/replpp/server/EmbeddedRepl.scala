package replpp.server

import dotty.tools.dotc.config.Printers.config
import dotty.tools.repl.State
import org.slf4j.{Logger, LoggerFactory}
import replpp.{Config, allPredefLines}

import java.io.{BufferedReader, ByteArrayOutputStream, InputStream, InputStreamReader, OutputStream, PipedInputStream, PipedOutputStream, PrintStream, PrintWriter}
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue, Semaphore}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import java.util.concurrent.Executors
import scala.concurrent.impl.Promise
import scala.util.{Failure, Success}

class EmbeddedRepl(config: Config) {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val singleThreadedJobExecutor: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  private val outStream = new ByteArrayOutputStream()

  private val replDriver = {
    val inheritedClasspath = System.getProperty("java.class.path")
    val compilerArgs = Array(
      "-classpath", inheritedClasspath,
      "-explain", // verbose scalac error messages
      "-deprecation",
      "-color", "never"
    )

    new ReplDriver(compilerArgs, new PrintStream(outStream))
  }

  private var state: State = {
    val state = replDriver.execute(allPredefLines(config))(using replDriver.initialState)
    logger.info(s"initialised REPL")
    outStream.reset()
    state
  }

  /** Execute `inputLines` in REPL (in single threaded ExecutorService) and provide Future for result callback */
  def queryAsync(inputLines: IterableOnce[String]): (UUID, Future[String]) = {
    val uuid = UUID.randomUUID()
    val future = Future {
      state = replDriver.execute(inputLines)(using state)
      val result = outStream.toString(StandardCharsets.UTF_8)
      outStream.reset()
      result
    } (using singleThreadedJobExecutor)

    (uuid, future)
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

/** Result of executing a query, containing in particular output received on standard out. */
case class QueryResult(output: String, uuid: UUID, success: Boolean) extends HasUUID
trait HasUUID { def uuid: UUID }
