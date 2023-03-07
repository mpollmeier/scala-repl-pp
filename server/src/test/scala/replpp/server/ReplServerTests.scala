package replpp.server

import cask.util.Logger.Console._
import castor.Context.Simple.global
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import requests.RequestFailedException
import ujson.Value.Value

import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.locks.{Lock, ReentrantLock}
import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._

// TODO reenable all tests
class ReplServerTests extends AnyWordSpec with Matchers {
  private val ValidBasicAuthHeaderVal: String = "Basic dXNlcm5hbWU6cGFzc3dvcmQ="
  private val DefaultPromiseAwaitTimeout: FiniteDuration = Duration(10, SECONDS)

  private def postQuery(host: String, query: String, authHeaderVal: String = ValidBasicAuthHeaderVal): Value = {
    val postResponse = requests.post(
      s"$host/query",
      data = ujson.Obj("query" -> query).toString,
      headers = Seq("authorization" -> authHeaderVal)
    )
    val res =
      if (postResponse.bytes.length > 0)
        ujson.read(postResponse.bytes)
      else
        ujson.Obj()
    res
  }

  private def getResponse(host: String, uuidParam: String, authHeaderVal: String = ValidBasicAuthHeaderVal): Value = {
    val uri = s"$host/result/${URLEncoder.encode(uuidParam, "utf-8")}"
    val getResponse = requests.get(uri, headers = Seq("authorization" -> authHeaderVal))
    ujson.read(getResponse.bytes)
  }

  /** These tests happen to fail on github actions for the windows runner with the following output: WARNING: Unable to
    * create a system terminal, creating a dumb terminal (enable debug logging for more information) Apr 21, 2022
    * 3:08:54 PM org.jboss.threads.Version <clinit> INFO: JBoss Threads version 3.1.0.Final Apr 21, 2022 3:08:55 PM
    * io.undertow.server.HttpServerExchange endExchange ERROR: UT005090: Unexpected failure
    * java.lang.NoClassDefFoundError: Could not initialize class org.xnio.channels.Channels at
    * io.undertow.io.UndertowOutputStream.close(UndertowOutputStream.java:348)
    *
    * This happens for both windows 2019 and 2022, and isn't reproducable elsewhere. Explicitly adding a dependency on
    * `org.jboss.xnio/xnio-api` didn't help, as well as other debug attempts. So we gave up and disabled this
    * specifically for github actions' windows runner.
    */
  val isGithubActions = scala.util.Properties.envOrElse("GITHUB_ACTIONS", "false").toLowerCase == "true"
  val isWindows       = scala.util.Properties.isWin

  if (isGithubActions && isWindows) {
    info("tests were cancelled because github actions windows doesn't support them for some unknown reason...")
  } else {

    "allow websocket connections to the `/connect` endpoint" ignore Fixture() { host =>
      val wsMsgPromise = scala.concurrent.Promise[String]()
      cask.util.WsClient.connect(s"$host/connect") { case cask.Ws.Text(msg) =>
        wsMsgPromise.success(msg)
      }
      val wsMsg = Await.result(wsMsgPromise.future, DefaultPromiseAwaitTimeout)
      wsMsg shouldBe "connected"
    }

    "allow posting a simple query without any websocket connections established" ignore Fixture() { host =>
      val postQueryResponse = postQuery(host, "1")
      postQueryResponse.obj.keySet should contain("success")
      val UUIDResponse = postQueryResponse("uuid").str
      UUIDResponse should not be empty
      postQueryResponse("success").bool shouldBe true
    }

    "disallow posting a query when request headers do not include a valid authentication value" ignore Fixture() { host =>
      assertThrows[RequestFailedException] {
        postQuery(host, "1", authHeaderVal = "Basic b4df00d")
      }
    }

    "return a valid JSON response when trying to retrieve the result of a query without a connection" ignore Fixture() {
      host =>
        val postQueryResponse = postQuery(host, "1")
        postQueryResponse.obj.keySet should contain("uuid")
        val UUIDResponse = postQueryResponse("uuid").str
        val getResultResponse = getResponse(host, UUIDResponse)
        getResultResponse.obj.keySet should contain("success")
        getResultResponse.obj.keySet should contain("err")
        getResultResponse("success").bool shouldBe false
        getResultResponse("err").str.length should not be 0
    }

    "allow fetching the result of a completed query using its UUID" ignore Fixture() { host =>
      val wsMsgPromise = scala.concurrent.Promise[String]()
      val connectedPromise = scala.concurrent.Promise[String]()
      cask.util.WsClient.connect(s"$host/connect") {
        case cask.Ws.Text(msg) if msg == "connected" =>
          connectedPromise.success(msg)
        case cask.Ws.Text(msg) =>
          wsMsgPromise.success(msg)
      }
      Await.result(connectedPromise.future, DefaultPromiseAwaitTimeout)
      val postQueryResponse = postQuery(host, "1")
      val queryUUID = postQueryResponse("uuid").str
      queryUUID.length should not be 0

      val queryResultWSMessage = Await.result(wsMsgPromise.future, DefaultPromiseAwaitTimeout)
      queryResultWSMessage.length should not be 0

      val getResultResponse = getResponse(host, queryUUID)
      getResultResponse.obj.keySet should contain("success")
      getResultResponse("uuid").str shouldBe queryResultWSMessage
      getResultResponse("stdout").str shouldBe "val res0: Int = 1\n"
    }

    "disallow fetching the result of a completed query with an invalid auth header" ignore Fixture() { host =>
      val wsMsgPromise = scala.concurrent.Promise[String]()
      val connectedPromise = scala.concurrent.Promise[String]()
      cask.util.WsClient.connect(s"$host/connect") {
        case cask.Ws.Text(msg) if msg == "connected" =>
          connectedPromise.success(msg)
        case cask.Ws.Text(msg) =>
          wsMsgPromise.success(msg)
      }
      Await.result(connectedPromise.future, DefaultPromiseAwaitTimeout)
      val postQueryResponse = postQuery(host, "1")
      val queryUUID = postQueryResponse("uuid").str
      queryUUID.length should not be 0

      val queryResultWSMessage = Await.result(wsMsgPromise.future, DefaultPromiseAwaitTimeout)
      queryResultWSMessage.length should not be 0

      assertThrows[RequestFailedException] {
        getResponse(host, queryUUID, "Basic b4df00d")
      }
    }

    "write a well-formatted message to a websocket connection when a query has finished evaluation" ignore Fixture() {
      host =>
        val wsMsgPromise = scala.concurrent.Promise[String]()
        val connectedPromise = scala.concurrent.Promise[String]()
        cask.util.WsClient.connect(s"$host/connect") {
          case cask.Ws.Text(msg) if msg == "connected" =>
            connectedPromise.success(msg)
          case cask.Ws.Text(msg) =>
            wsMsgPromise.success(msg)
        }
        Await.result(connectedPromise.future, DefaultPromiseAwaitTimeout)

        val postQueryResponse = postQuery(host, "1")
        val queryUUID = postQueryResponse("uuid").str
        queryUUID.length should not be 0

        val queryResultWSMessage = Await.result(wsMsgPromise.future, DefaultPromiseAwaitTimeout)
        queryResultWSMessage.length should not be 0

        val getResultResponse = getResponse(host, queryUUID)
        getResultResponse.obj.keySet should contain("success")
        getResultResponse.obj.keySet should contain("stdout")
        getResultResponse.obj.keySet should not contain "err"
        getResultResponse("uuid").str shouldBe queryResultWSMessage
        getResultResponse("stdout").str shouldBe "val res0: Int = 1\n"
    }

    "write a well-formatted message to a websocket connection when a query failed evaluation" ignore Fixture() { host =>
      val wsMsgPromise = scala.concurrent.Promise[String]()
      val connectedPromise = scala.concurrent.Promise[String]()
      cask.util.WsClient.connect(s"$host/connect") {
        case cask.Ws.Text(msg) if msg == "connected" =>
          connectedPromise.success(msg)
        case cask.Ws.Text(msg) =>
          wsMsgPromise.success(msg)
      }
      Await.result(connectedPromise.future, DefaultPromiseAwaitTimeout)

      val postQueryResponse = postQuery(host, "if else for loop soup // i.e., an invalid Ammonite query")
      val queryUUID = postQueryResponse("uuid").str
      queryUUID.length should not be 0

      val wsMsg = Await.result(wsMsgPromise.future, DefaultPromiseAwaitTimeout)
      wsMsg.length should not be 0

      val resp = getResponse(host, queryUUID)
      resp.obj.keySet should contain("success")
      resp.obj.keySet should contain("stdout")
      resp.obj.keySet should not contain "err"

      resp("success").bool shouldBe true
      resp("uuid").str shouldBe wsMsg
      resp("stdout").str.length should not be 0
    }

    "write a well-formatted message to a websocket connection when a query containing an invalid char is submitted" ignore Fixture() {
      host =>
        val wsMsgPromise = scala.concurrent.Promise[String]()
        val connectedPromise = scala.concurrent.Promise[String]()
        cask.util.WsClient.connect(s"$host/connect") {
          case cask.Ws.Text(msg) if msg == "connected" =>
            connectedPromise.success(msg)
          case cask.Ws.Text(msg) =>
            wsMsgPromise.success(msg)
        }
        Await.result(connectedPromise.future, DefaultPromiseAwaitTimeout)

        val postQueryResponse = postQuery(host, "@1")
        val queryUUID = postQueryResponse("uuid").str
        queryUUID.length should not be 0

        val wsMsg = Await.result(wsMsgPromise.future, DefaultPromiseAwaitTimeout)
        wsMsg.length should not be 0

        val resp = getResponse(host, queryUUID)
        resp.obj.keySet should contain("success")
        resp.obj.keySet should contain("stdout")

        resp("success").bool shouldBe true
        resp("uuid").str shouldBe wsMsg
        resp("stdout").str.length should not be 0
    }

    "receive error when attempting to retrieve result with invalid uuid" ignore Fixture() { host =>
      val connectedPromise = scala.concurrent.Promise[String]()
      cask.util.WsClient.connect(s"$host/connect") { case cask.Ws.Text(msg) =>
        connectedPromise.success(msg)
      }
      Await.result(connectedPromise.future, Duration(1, SECONDS))
      val getResultResponse = getResponse(host, UUID.randomUUID().toString)
      getResultResponse.obj.keySet should contain("success")
      getResultResponse.obj.keySet should contain("err")
      getResultResponse("success").bool shouldBe false
    }

    "return a valid JSON response when calling /result with incorrectly-formatted UUID parameter" ignore Fixture() { host =>
      val connectedPromise = scala.concurrent.Promise[String]()
      cask.util.WsClient.connect(s"$host/connect") { case cask.Ws.Text(msg) =>
        connectedPromise.success(msg)
      }
      Await.result(connectedPromise.future, Duration(1, SECONDS))
      val getResultResponse = getResponse(host, "INCORRECTLY_FORMATTED_UUID_PARAM")
      getResultResponse.obj.keySet should contain("success")
      getResultResponse.obj.keySet should contain("err")
      getResultResponse("success").bool shouldBe false
      getResultResponse("err").str.length should not equal 0
    }

    "return websocket responses for all queries when posted quickly in a large number" ignore Fixture() { host =>
      val numQueries = 10
      val correctNumberOfUUIDsReceived = scala.concurrent.Promise[String]()
      val wsUUIDs = ListBuffer[String]()

      val rtl: Lock = new ReentrantLock()
      val connectedPromise = scala.concurrent.Promise[String]()
      cask.util.WsClient.connect(s"$host/connect") { case cask.Ws.Text(msg) =>
        if (msg == "connected") {
          connectedPromise.success(msg)
        } else {
          rtl.lock()
          try {
            wsUUIDs += msg
          } finally {
            rtl.unlock()
            if (wsUUIDs.size == numQueries) {
              correctNumberOfUUIDsReceived.success("")
            }
          }
        }
      }
      Await.result(connectedPromise.future, DefaultPromiseAwaitTimeout)

      val postQueriesResponseUUIDs =
        for (_ <- 1 to numQueries) yield {
          val postQueryResponse = postQuery(host, "1")
          postQueryResponse("uuid").str
        }

      Await.result(correctNumberOfUUIDsReceived.future, DefaultPromiseAwaitTimeout * numQueries.toLong)
      wsUUIDs.toSet should be(postQueriesResponseUUIDs.toSet)
    }

    "return websocket responses for all queries when some are invalid" ignore Fixture() { host =>
      val queries = List("1", "1 + 1", "open(", "open)", "open{", "open}")
      val correctNumberOfUUIDsReceived = scala.concurrent.Promise[String]()
      val wsUUIDs = ListBuffer[String]()
      val connectedPromise = scala.concurrent.Promise[String]()

      val rtl: Lock = new ReentrantLock()
      cask.util.WsClient.connect(s"$host/connect") { case cask.Ws.Text(msg) =>
        if (msg == "connected") {
          connectedPromise.success(msg)
        } else {
          rtl.lock()
          try {
            wsUUIDs += msg
          } finally {
            rtl.unlock()
            if (wsUUIDs.size == queries.size) {
              correctNumberOfUUIDsReceived.success("")
            }
          }
        }
      }
      Await.result(connectedPromise.future, DefaultPromiseAwaitTimeout)

      val postQueriesResponseUUIDs = {
        queries
          .map(q => {
            val res = postQuery(host, q)
            res("uuid").str
          })
      }
      Await.result(correctNumberOfUUIDsReceived.future, DefaultPromiseAwaitTimeout * queries.size.toLong)
      wsUUIDs.toSet should be(postQueriesResponseUUIDs.toSet)
    }
  }
}

object Fixture {

  def apply[T]()(f: String => T): T = {
    val embeddedRepl = new EmbeddedRepl()
    embeddedRepl.start()

    val host = "localhost"
    val port = 8081
    val authUsername = "username"
    val authPassword = "password"
    val httpEndpoint = "http://" + host + ":" + port.toString
    val replServer = new ReplServer(embeddedRepl, host, port, authUsername, authPassword)
    val server = io.undertow.Undertow.builder
      .addHttpListener(replServer.port, replServer.host)
      .setHandler(replServer.defaultHandler)
      .build
    server.start()
    val res =
      try {
        f(httpEndpoint)
      }
      finally {
        server.stop()
        embeddedRepl.shutdown()
      }
    res
  }
}
