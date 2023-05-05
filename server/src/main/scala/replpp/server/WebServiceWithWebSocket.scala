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


trait HasUUID { def uuid: UUID }

abstract class WebServiceWithWebSocket[T <: HasUUID](
                                                      serverHost: String,
                                                      serverPort: Int,
                                                      serverAuthUsername: String = "",
                                                      serverAuthPassword: String = ""
                                                    ) extends cask.MainRoutes {
  protected val logger: Logger = LoggerFactory.getLogger(getClass)

  class basicAuth extends cask.RawDecorator {
    def wrapFunction(ctx: Request, delegate: Delegate): Result[Raw] = {
      val authString                           = requestToAuthString(ctx)
      val Array(user, password): Array[String] = authStringToUserAndPwd(authString)
      val isAuthorized =
        if (serverAuthUsername == "" && serverAuthPassword == "")
          true
        else
          user == serverAuthUsername && password == serverAuthPassword
      delegate(Map("isAuthorized" -> isAuthorized))
    }

    private def requestToAuthString(ctx: Request): String = {
      try {
        val authHeader     = ctx.exchange.getRequestHeaders.get("authorization").getFirst
        val strippedHeader = authHeader.replaceFirst("Basic ", "")
        new String(Base64.getDecoder.decode(strippedHeader))
      } catch {
        case _: Exception => ""
      }
    }

    private def authStringToUserAndPwd(authString: String): Array[String] = {
      authString.split(":", 2) match {
        case array if array.length == 2 => array
        case _ => Array("", "")
      }
    }
  }

  override def port: Int = serverPort

  override def host: String = serverHost

  var openConnections                     = Set.empty[cask.WsChannelActor]
  val resultMap                           = new ConcurrentHashMap[UUID, (T, Boolean)]()
  val unauthorizedResponse: Response[Obj] = Response(ujson.Obj(), 401, headers = Seq("WWW-Authenticate" -> "Basic"))

  def handler(): cask.WebsocketResult = {
    cask.WsHandler { connection =>
      connection.send(cask.Ws.Text("connected"))
      openConnections += connection
      cask.WsActor {
        case cask.Ws.Error(e) =>
          logger.error("Connection error: " + e.getMessage)
          openConnections -= connection
        case cask.Ws.Close(_, _) | cask.Ws.ChannelClosed() =>
          logger.debug("Connection closed.")
          openConnections -= connection
      }
    }
  }

  def getResult(uuidParam: String)(isAuthorized: Boolean): Response[Obj] = {
    if (!isAuthorized) {
      unauthorizedResponse
    } else {
      Try(UUID.fromString(uuidParam)) match {
        case Success(uuid) if !resultMap.containsKey(uuid) =>
          Response(ujson.Obj("success" -> false, "err" -> "No result (yet?) found for specified UUID"), 200)
        case Success(uuid) =>
          val (result, success) = resultMap.remove(uuid)
          Response(resultToJson(result, success), 200)
        case Failure(_) =>
          Response(ujson.Obj("success" -> false, "err" -> "UUID parameter is incorrectly formatted"), 200)
      }
    }
  }

  def returnResult(result: T): Unit = {
    resultMap.put(result.uuid, (result, true))
    openConnections.foreach { connection =>
      connection.send(cask.Ws.Text(result.uuid.toString))
    }
    Response(ujson.Obj("success" -> true, "uuid" -> result.uuid.toString), 200)
  }

  def resultToJson(result: T, success: Boolean): Obj

  initialize()
}


