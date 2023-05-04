package replpp.server

import dotty.tools.repl.State
import org.slf4j.{Logger, LoggerFactory}

import java.io.{BufferedReader, InputStream, InputStreamReader, PipedInputStream, PipedOutputStream, PrintStream, PrintWriter}
import java.util.UUID
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue, Semaphore}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import java.util.concurrent.Executors
import scala.concurrent.impl.Promise
import scala.util.{Failure, Success}

/** Result of executing a query, containing in particular output received on standard out. */
case class QueryResult(out: String, uuid: UUID, isSuccessful: Boolean) extends HasUUID

trait HasUUID {
  def uuid: UUID
}

//private[server] case class Job(uuid: UUID, query: String, observer: QueryResult => Unit)

class EmbeddedRepl(predefCode: IterableOnce[String], verbose: Boolean = false) {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val singleThreadedJobExecutor: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  private val replDriver = {
    val inheritedClasspath = System.getProperty("java.class.path")
    val compilerArgs = Array(
      "-classpath", inheritedClasspath,
      "-explain", // verbose scalac error messages
      "-deprecation",
      "-color", "never"
    )

    // TODO pass our own outStream that we can parse
//    new ReplDriver(compilerArgs, new PrintStream(outStream))
    new ReplDriver(compilerArgs, System.out)
  }

  private var state: State = {
    println("XXX2a: prefore predef println")
    logger.info("XXX2a: prefore predef")
    val ret = replDriver.execute(predefCode)(using replDriver.initialState)
    println("XXX2b println: predef executed: objIndex=" + ret.objectIndex)
    logger.info("XXX2b: predef executed: objIndex=" + ret.objectIndex)
    ret
  }

  // TODO bring back jobqueue thread that submits jobs: start and stop


  /** Submit query `q` to shell and call `observer` when the result is ready.
    */
  def queryAsync(inputLines: IterableOnce[String]): (UUID, Future[String]) = {
    val uuid = UUID.randomUUID()
    val future = Future {
      // TODO handle nicer, e.g. abstract in separate ReplDriver that handles input, state, output
      state = replDriver.execute(inputLines)(using state)
      "TODO get rendered output"
    }(using singleThreadedJobExecutor)

    (uuid, future)
  }

  /** Submit query `q` to the shell and return result.
    */
  def query(q: String): QueryResult = {
    ???
//    val mutex               = new Semaphore(0)
//    var result: QueryResult = null
//    queryAsync(q) { r =>
//      result = r
//      mutex.release()
//    }
//    mutex.acquire()
//    result
  }

  /** Shutdown the embedded shell and associated threads.
    */
  def shutdown(): Unit = {
    logger.info("shutting down")
    singleThreadedJobExecutor.shutdown()
  }

}
