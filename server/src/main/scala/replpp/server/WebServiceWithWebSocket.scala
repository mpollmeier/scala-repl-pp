package replpp.server

import cask.model.Response.Raw
import cask.model.{Request, Response}
import cask.router.Result
import org.slf4j.{Logger, LoggerFactory}
import ujson.Obj

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.{Base64, UUID}
import scala.util.{Failure, Success, Try}

trait HasUUID { def uuid: UUID }

case class UsernamePasswordAuth(username: String, password: String)

abstract class WebServiceWithWebSocket[T <: HasUUID](
  override val host: String,
  override val port: Int,
  authenticationMaybe: Option[UsernamePasswordAuth] = None) extends cask.MainRoutes {
  protected val logger: Logger = LoggerFactory.getLogger(getClass)

  class basicAuth extends cask.RawDecorator {
    private lazy val utf8 = StandardCharsets.UTF_8
    def wrapFunction(request: Request, delegate: Delegate): Result[Raw] = {
      val isAuthorized = authenticationMaybe match {
        case None => true // no authorization required
        case Some(requiredAuth) =>
          parseAuthentication(request) match {
            case None => false // no authentication provided
            case Some(providedAuth) => areEqual(providedAuth, requiredAuth)
          }
      }
      delegate(Map("isAuthorized" -> isAuthorized))
    }

    private def parseAuthentication(request: Request): Option[UsernamePasswordAuth] =
      Try {
        val authHeader = request.exchange.getRequestHeaders.get("authorization").getFirst
        val strippedHeader = authHeader.replaceFirst("Basic ", "")
        val authString = new String(Base64.getDecoder.decode(strippedHeader))
        authString.split(":", 2) match {
          case Array(username, password) => Some(UsernamePasswordAuth(username, password))
          case _ => None
        }
      }.toOption.flatten

    /* constant-time comparison that prevents leaking the expected password through a timing side-channel */
    private def areEqual(providedAuth: UsernamePasswordAuth, expectedAuth: UsernamePasswordAuth): Boolean =
      MessageDigest.isEqual(providedAuth.toString.getBytes(utf8), expectedAuth.toString.getBytes(utf8))
  }

  private var openConnections        = Set.empty[cask.WsChannelActor]
  private val resultMap              = new ConcurrentHashMap[UUID, (T, Boolean)]()
  protected val unauthorizedResponse = Response(ujson.Obj(), 401, headers = Seq("WWW-Authenticate" -> "Basic"))

  def handler(): cask.WebsocketResult = {
    cask.WsHandler { connection =>
      connection.send(cask.Ws.Text("connected"))
      openConnections += connection
      cask.WsActor {
        case cask.Ws.Error(e) =>
          logger.error("Connection error", e)
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


