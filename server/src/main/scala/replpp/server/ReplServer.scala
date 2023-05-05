package replpp.server

import cask.model.Response.Raw
import cask.model.{Request, Response}
import cask.router.Result
import org.slf4j.{Logger, LoggerFactory}
import replpp.{Config, allPredefCode, allPredefLines}
import ujson.Obj

import java.io.{PrintWriter, StringWriter}
import java.util.concurrent.ConcurrentHashMap
import java.util.{Base64, UUID}
import scala.util.{Failure, Success, Try}

object ReplServer {
  /** Result of executing a query, containing in particular output received on standard out. */
  case class QueryResult(output: String, uuid: UUID, success: Boolean) extends HasUUID

  protected val logger: Logger = LoggerFactory.getLogger(getClass)

  def startHttpServer(config: Config): Unit = {
    val embeddedRepl = new EmbeddedRepl(config)
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      logger.info("Shutting down server...")
      embeddedRepl.shutdown()
    }))

    val server = new ReplServer(
      embeddedRepl,
      config.serverHost,
      config.serverPort,
      config.serverAuthUsername,
      config.serverAuthPassword
    )
    logger.info("Starting REPL server ...")
    try {
      server.main(Array.empty)
    } catch {
      case _: java.net.BindException =>
        logger.error(s"Could not bind socket on port ${config.serverPort} - exiting.")
        embeddedRepl.shutdown()
        System.exit(1)
      case e: Throwable =>
        logger.error("Unhandled exception thrown while attempting to start server - exiting", e)

        embeddedRepl.shutdown()
        System.exit(1)
    }
  }
}

class ReplServer(repl: EmbeddedRepl,
                 serverHost: String,
                 serverPort: Int,
                 serverAuthUsername: String = "",
                 serverAuthPassword: String = ""
) extends WebServiceWithWebSocket[QueryResult](serverHost, serverPort, serverAuthUsername, serverAuthPassword) {

  @cask.websocket("/connect")
  override def handler(): cask.WebsocketResult = super.handler()

  @basicAuth()
  @cask.get("/result/:uuidParam")
  override def getResult(uuidParam: String)(isAuthorized: Boolean): Response[Obj] = {
    val response = super.getResult(uuidParam)(isAuthorized)
    logger.debug(s"GET /result/$uuidParam: statusCode=${response.statusCode}")
    response
  }

  @basicAuth()
  @cask.postJson("/query")
  def postQuery(query: String)(isAuthorized: Boolean): Response[Obj] = {
    if (!isAuthorized) unauthorizedResponse
    else {
      val (uuid, resultFuture) = repl.queryAsync(query.linesIterator)
      logger.debug(s"query[uuid=$uuid, length=${query.length}]: submitted to queue")
      resultFuture.onComplete {
        case Success(output) =>
          logger.debug(s"query[uuid=$uuid]: got result (length=${output.length})")
          returnResult(QueryResult(output, uuid, success = true))
        case Failure(exception) =>
          logger.info(s"query[uuid=$uuid] failed with $exception")
          returnResult(QueryResult(render(exception), uuid, success = false))
      }
      Response(ujson.Obj("success" -> true, "uuid" -> uuid.toString), 200)
    }
  }

  @basicAuth()
  @cask.postJson("/query-sync")
  def postQuerySimple(query: String)(isAuthorized: Boolean): Response[Obj] = {
    if (!isAuthorized) unauthorizedResponse
    else {
      logger.debug(s"POST /query-sync query.length=${query.length}")
      val result = repl.query(query.linesIterator)
      logger.debug(s"query-sync: got result: length=${result.output.length}")
      Response(ujson.Obj("success" -> true, "out" -> result.output, "uuid" -> result.uuid.toString), 200)
    }
  }

  override def resultToJson(result: QueryResult, success: Boolean): Obj = {
    ujson.Obj("success" -> success, "uuid" -> result.uuid.toString, "stdout" -> result.output)
  }

  private def render(throwable: Throwable): String = {
    val sw = new StringWriter
    throwable.printStackTrace(new PrintWriter(sw))
    throwable.getMessage() + System.lineSeparator() + sw.toString()
  }

  initialize()
}

