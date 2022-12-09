package replpp.server

import java.io.{BufferedReader, PrintWriter}
import java.lang.System.lineSeparator
import java.util.UUID
import java.util.concurrent.BlockingQueue
import org.slf4j.{Logger, LoggerFactory}
import scala.util.Try

class UserRunnable(queue: BlockingQueue[Job], writer: PrintWriter, reader: BufferedReader, verbose: Boolean = false)
  extends Runnable {
  private val logger    = LoggerFactory.getLogger(classOf[UserRunnable])
  private val endMarker = """.*END: ([0-9a-f\-]+)""".r

  override def run(): Unit = {
    try {
      var terminate = false
      while (!(terminate && queue.isEmpty)) {
        val job = queue.take()
        if (isTerminationMarker(job)) {
          terminate = true
        } else {
          if (verbose) println(s"executing: $job")
          sendQueryToEmbeddedRepl(job)
          val stdoutPair = stdOutUpToMarker()
          val stdOutput  = stdoutPair.get
          val result = QueryResult(stdOutput, job.uuid)
          if (verbose) println(s"result: $result")
          job.observer(result)
        }
      }
    } catch {
      case _: InterruptedException =>
        logger.info("Interrupted WriterThread")
    }
    logger.debug("WriterThread terminated gracefully")
  }

  private def isTerminationMarker(job: Job): Boolean = {
    job.uuid == null && job.query == null
  }

  private def sendQueryToEmbeddedRepl(job: Job): Unit = {
    writer.println(job.query.trim)
    writer.println(s""""END: ${job.uuid}"""")
    writer.flush()
  }

  private def stdOutUpToMarker(): Option[String] = {
    var currentOutput: String = ""
    var line                  = reader.readLine()
    while (line != null) {
      if (line.nonEmpty) {
        val uuid = uuidFromLine(line)
        if (uuid.isEmpty) {
          currentOutput += line + lineSeparator
        } else {
          return Some(currentOutput)
        }
      }
      line = reader.readLine()
    }
    None
  }

  private def uuidFromLine(line: String): Iterator[UUID] = {
    endMarker.findAllIn(line).matchData.flatMap { m =>
      Try { UUID.fromString(m.group(1)) }.toOption
    }
  }

}
