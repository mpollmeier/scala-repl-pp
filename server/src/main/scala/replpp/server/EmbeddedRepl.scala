package replpp.server

import dotty.tools.repl.State
import org.slf4j.LoggerFactory
import replpp.Colors.BlackWhite
import replpp.{ReplDriverBase, pwd}

import java.io.*
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}

class EmbeddedRepl(
  compilerArgs: Array[String],
  runBeforeCode: Seq[String] = Nil,
  runAfterCode: Seq[String] = Nil,
  verbose: Boolean = false) {
  private val logger = LoggerFactory.getLogger(getClass)

  /** repl and compiler output ends up in this replOutputStream */
  private val replOutputStream = new ByteArrayOutputStream()

  private val replDriver = ReplDriver(compilerArgs, new PrintStream(replOutputStream), classLoader = None)

  private var state: State = {
    if (runBeforeCode.nonEmpty) {
      if (verbose) logger.info(s"executing runBeforeCode: \n${runBeforeCode.mkString("\n")}")
      replDriver.execute(runBeforeCode)
    } else
      replDriver.initialState
  }

  private val singleThreadedJobExecutor: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  /** Execute `inputLines` in REPL (in single threaded ExecutorService) and provide Future for result callback */
  def queryAsync(code: String): (UUID, Future[String]) =
    queryAsync(code.linesIterator)

  /** Execute `inputLines` in REPL (in single threaded ExecutorService) and provide Future for result callback */
  def queryAsync(inputLines: IterableOnce[String]): (UUID, Future[String]) = {
    val uuid = UUID.randomUUID()
    val future = Future {
      val lines = inputLines.iterator.toSeq
      if (verbose) logger.info(s"executing: \n${lines.mkString("\n")}")
      state = replDriver.execute(lines)(using state)
      readAndResetReplOutputStream()
    } (using singleThreadedJobExecutor)

    (uuid, future)
  }

  private def readAndResetReplOutputStream(): String = {
    val result = replOutputStream.toString(StandardCharsets.UTF_8)
    replOutputStream.reset()
    result
  }

  /** Submit query to the repl, await and return results. */
  def query(code: String): QueryResult =
    query(code.linesIterator)

  /** Submit query to the repl, await and return results. */
  def query(inputLines: IterableOnce[String]): QueryResult = {
    val (uuid, futureResult) = queryAsync(inputLines)
    val result = Await.result(futureResult, Duration.Inf)
    QueryResult(result, uuid, success = true)
  }

  /** Shutdown the embedded shell and associated threads.
    */
  def shutdown(): Unit = {
    logger.info("shutting down")
    if (runAfterCode.nonEmpty) {
      if (verbose) logger.info(s"executing: $runAfterCode")
      state = replDriver.execute(runAfterCode)(using state)
      val output = readAndResetReplOutputStream()
      logger.info(output)
    }
    singleThreadedJobExecutor.shutdown()
  }
}

class ReplDriver(args: Array[String], out: PrintStream, classLoader: Option[ClassLoader])
  extends ReplDriverBase(args, out, maxHeight = None, classLoader)(using BlackWhite) {
  def execute(inputLines: IterableOnce[String])(using state: State = initialState): State = {
    interpretInput(inputLines, state, pwd)
  }
}
