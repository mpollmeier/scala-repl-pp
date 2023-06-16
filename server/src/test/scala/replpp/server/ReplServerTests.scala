package replpp.server

import cask.util.Logger.Console.*
import castor.Context.Simple.global
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import replpp.Config
import requests.RequestFailedException
import ujson.Value.Value

import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.locks.{Lock, ReentrantLock}
import scala.collection.mutable.ListBuffer
import scala.concurrent.*
import scala.concurrent.duration.*

class ReplServerTests extends AnyWordSpec with Matchers {
  private val ValidBasicAuthHeaderVal: String = "Basic dXNlcm5hbWU6cGFzc3dvcmQ="
  private val DefaultPromiseAwaitTimeout: FiniteDuration = Duration(10, SECONDS)

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

    "asynchronous api" should {
      "allow websocket connections to the `/connect` endpoint" in Fixture() { url =>
        val wsMsgPromise = scala.concurrent.Promise[String]()
        cask.util.WsClient.connect(s"$url/connect") { case cask.Ws.Text(msg) =>
          wsMsgPromise.success(msg)
        }
        val wsMsg = Await.result(wsMsgPromise.future, DefaultPromiseAwaitTimeout)
        wsMsg shouldBe "connected"
      }

      "allow posting a simple query without any websocket connections established" in Fixture() { url =>
        val postQueryResponse = postQueryAsync(url, "1")
        postQueryResponse.obj.keySet should contain("success")
        val UUIDResponse = postQueryResponse("uuid").str
        UUIDResponse should not be empty
        postQueryResponse("success").bool shouldBe true
      }

      "disallow posting a query when request headers do not include a valid authentication value" in Fixture() { url =>
        assertThrows[RequestFailedException] {
          postQueryAsync(url, "1", authHeaderVal = "Basic b4df00d")
        }
      }

      "return a valid JSON response when trying to retrieve the result of a query without a connection" in Fixture() {
        url =>
          val postQueryResponse = postQueryAsync(url, "val x = 10")
          postQueryResponse.obj.keySet should contain("uuid")
          val UUIDResponse = postQueryResponse("uuid").str
          val response = getResponse(url, UUIDResponse)
          response.obj.keySet should contain("success")
          response.obj.keySet should contain("err")
          response("success").bool shouldBe false
          response("err").str.length should not be 0
      }

      if (isWindows) {
        info("tests were cancelled currently fail on windows - not sure why - I'm testing the `--server` mode manually for now and will replace these tests in the future")
      } else {
        "allow fetching the result of a completed query using its UUID" in Fixture() { url =>
          val wsMsgPromise = scala.concurrent.Promise[String]()
          val connectedPromise = scala.concurrent.Promise[String]()
          cask.util.WsClient.connect(s"$url/connect") {
            case cask.Ws.Text(msg) if msg == "connected" =>
              connectedPromise.success(msg)
            case cask.Ws.Text(msg) =>
              wsMsgPromise.success(msg)
          }
          Await.result(connectedPromise.future, DefaultPromiseAwaitTimeout)
          val postQueryResponse = postQueryAsync(url, "1")
          val queryUUID = postQueryResponse("uuid").str
          queryUUID.length should not be 0

          val queryResultWSMessage = Await.result(wsMsgPromise.future, DefaultPromiseAwaitTimeout)
          queryResultWSMessage.length should not be 0

          val getResultResponse = getResponse(url, queryUUID)
          getResultResponse.obj.keySet should contain("success")
          getResultResponse("uuid").str shouldBe queryResultWSMessage
          getResultResponse("stdout").str shouldBe "val res0: Int = 1\n"
        }

        "use predefined code" in Fixture("val foo = 40") { url =>
          val wsMsgPromise = scala.concurrent.Promise[String]()
          val connectedPromise = scala.concurrent.Promise[String]()
          cask.util.WsClient.connect(s"$url/connect") {
            case cask.Ws.Text(msg) if msg == "connected" =>
              connectedPromise.success(msg)
            case cask.Ws.Text(msg) =>
              wsMsgPromise.success(msg)
          }
          Await.result(connectedPromise.future, DefaultPromiseAwaitTimeout)
          val postQueryResponse = postQueryAsync(url, "val bar = foo + 2")
          val queryUUID = postQueryResponse("uuid").str
          queryUUID.length should not be 0

          val queryResultWSMessage = Await.result(wsMsgPromise.future, DefaultPromiseAwaitTimeout)
          queryResultWSMessage.length should not be 0

          val getResultResponse = getResponse(url, queryUUID)
          getResultResponse.obj.keySet should contain("success")
          getResultResponse("uuid").str shouldBe queryResultWSMessage
          getResultResponse("stdout").str shouldBe "val bar: Int = 42\n"
        }

        "disallow fetching the result of a completed query with an invalid auth header" in Fixture() { url =>
          val wsMsgPromise = scala.concurrent.Promise[String]()
          val connectedPromise = scala.concurrent.Promise[String]()
          cask.util.WsClient.connect(s"$url/connect") {
            case cask.Ws.Text(msg) if msg == "connected" =>
              connectedPromise.success(msg)
            case cask.Ws.Text(msg) =>
              wsMsgPromise.success(msg)
          }
          Await.result(connectedPromise.future, DefaultPromiseAwaitTimeout)
          val postQueryResponse = postQueryAsync(url, "1")
          val queryUUID = postQueryResponse("uuid").str
          queryUUID.length should not be 0

          val queryResultWSMessage = Await.result(wsMsgPromise.future, DefaultPromiseAwaitTimeout)
          queryResultWSMessage.length should not be 0

          assertThrows[RequestFailedException] {
            getResponse(url, queryUUID, "Basic b4df00d")
          }
        }

        "write a well-formatted message to a websocket connection when a query has finished evaluation" in Fixture() {
          url =>
            val wsMsgPromise = scala.concurrent.Promise[String]()
            val connectedPromise = scala.concurrent.Promise[String]()
            cask.util.WsClient.connect(s"$url/connect") {
              case cask.Ws.Text(msg) if msg == "connected" =>
                connectedPromise.success(msg)
              case cask.Ws.Text(msg) =>
                wsMsgPromise.success(msg)
            }
            Await.result(connectedPromise.future, DefaultPromiseAwaitTimeout)

            val postQueryResponse = postQueryAsync(url, "1")
            val queryUUID = postQueryResponse("uuid").str
            queryUUID.length should not be 0

            val queryResultWSMessage = Await.result(wsMsgPromise.future, DefaultPromiseAwaitTimeout)
            queryResultWSMessage.length should not be 0

            val getResultResponse = getResponse(url, queryUUID)
            getResultResponse.obj.keySet should contain("success")
            getResultResponse.obj.keySet should contain("stdout")
            getResultResponse.obj.keySet should not contain "err"
            getResultResponse("uuid").str shouldBe queryResultWSMessage
            getResultResponse("stdout").str shouldBe "val res0: Int = 1\n"
        }
      }

      "write a well-formatted message to a websocket connection when a query failed evaluation" in Fixture() { url =>
        val wsMsgPromise = scala.concurrent.Promise[String]()
        val connectedPromise = scala.concurrent.Promise[String]()
        cask.util.WsClient.connect(s"$url/connect") {
          case cask.Ws.Text(msg) if msg == "connected" =>
            connectedPromise.success(msg)
          case cask.Ws.Text(msg) =>
            wsMsgPromise.success(msg)
        }
        Await.result(connectedPromise.future, DefaultPromiseAwaitTimeout)

        val postQueryResponse = postQueryAsync(url, "if else for loop soup // i.e., an invalid query")
        val queryUUID = postQueryResponse("uuid").str
        queryUUID.length should not be 0

        val wsMsg = Await.result(wsMsgPromise.future, DefaultPromiseAwaitTimeout)
        wsMsg.length should not be 0

        val resp = getResponse(url, queryUUID)
        resp.obj.keySet should contain("success")
        resp.obj.keySet should contain("stdout")
        resp.obj.keySet should not contain "err"

        resp("success").bool shouldBe true
        resp("uuid").str shouldBe wsMsg
        resp("stdout").str.length should not be 0
      }

      "write a well-formatted message to a websocket connection when a query containing an invalid char is submitted" in Fixture() {
        url =>
          val wsMsgPromise = scala.concurrent.Promise[String]()
          val connectedPromise = scala.concurrent.Promise[String]()
          cask.util.WsClient.connect(s"$url/connect") {
            case cask.Ws.Text(msg) if msg == "connected" =>
              connectedPromise.success(msg)
            case cask.Ws.Text(msg) =>
              wsMsgPromise.success(msg)
          }
          Await.result(connectedPromise.future, DefaultPromiseAwaitTimeout)

          val postQueryResponse = postQueryAsync(url, "@1")
          val queryUUID = postQueryResponse("uuid").str
          queryUUID.length should not be 0

          val wsMsg = Await.result(wsMsgPromise.future, DefaultPromiseAwaitTimeout)
          wsMsg.length should not be 0

          val resp = getResponse(url, queryUUID)
          resp.obj.keySet should contain("success")
          resp.obj.keySet should contain("stdout")

          resp("success").bool shouldBe true
          resp("uuid").str shouldBe wsMsg
          resp("stdout").str.length should not be 0
      }

      "receive error when attempting to retrieve result with invalid uuid" in Fixture() { url =>
        val connectedPromise = scala.concurrent.Promise[String]()
        cask.util.WsClient.connect(s"$url/connect") { case cask.Ws.Text(msg) =>
          connectedPromise.success(msg)
        }
        Await.result(connectedPromise.future, Duration(1, SECONDS))
        val getResultResponse = getResponse(url, UUID.randomUUID().toString)
        getResultResponse.obj.keySet should contain("success")
        getResultResponse.obj.keySet should contain("err")
        getResultResponse("success").bool shouldBe false
      }

      "return a valid JSON response when calling /result with incorrectly-formatted UUID parameter" in Fixture() { url =>
        val connectedPromise = scala.concurrent.Promise[String]()
        cask.util.WsClient.connect(s"$url/connect") { case cask.Ws.Text(msg) =>
          connectedPromise.success(msg)
        }
        Await.result(connectedPromise.future, Duration(1, SECONDS))
        val getResultResponse = getResponse(url, "INCORRECTLY_FORMATTED_UUID_PARAM")
        getResultResponse.obj.keySet should contain("success")
        getResultResponse.obj.keySet should contain("err")
        getResultResponse("success").bool shouldBe false
        getResultResponse("err").str.length should not equal 0
      }

      "return websocket responses for all queries when posted quickly in a large number" in Fixture() { url =>
        val numQueries = 10
        val correctNumberOfUUIDsReceived = scala.concurrent.Promise[String]()
        val wsUUIDs = ListBuffer[String]()

        val rtl: Lock = new ReentrantLock()
        val connectedPromise = scala.concurrent.Promise[String]()
        cask.util.WsClient.connect(s"$url/connect") { case cask.Ws.Text(msg) =>
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
            val postQueryResponse = postQueryAsync(url, "1")
            postQueryResponse("uuid").str
          }

        Await.result(correctNumberOfUUIDsReceived.future, DefaultPromiseAwaitTimeout * numQueries.toLong)
        wsUUIDs.toSet should be(postQueriesResponseUUIDs.toSet)
      }

      "return websocket responses for all queries when some are invalid" in Fixture() { url =>
        val queries = List("1", "1 + 1", "open(", "open)", "open{", "open}")
        val correctNumberOfUUIDsReceived = scala.concurrent.Promise[String]()
        val wsUUIDs = ListBuffer[String]()
        val connectedPromise = scala.concurrent.Promise[String]()

        val rtl: Lock = new ReentrantLock()
        cask.util.WsClient.connect(s"$url/connect") { case cask.Ws.Text(msg) =>
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

        val postQueriesResponseUUIDs = queries.map { query =>
          postQueryAsync(url, query)("uuid").str
        }
        Await.result(correctNumberOfUUIDsReceived.future, DefaultPromiseAwaitTimeout * queries.size.toLong)
        wsUUIDs.toSet should be(postQueriesResponseUUIDs.toSet)
      }
    }

    if (isWindows) {
      info("tests were cancelled currently fail on windows - not sure why - I'm testing the `--server` mode manually for now and will replace these tests in the future")
    } else {
      "synchronous api" should {
        "work for simple case" in Fixture() { url =>
          val response = postQuerySync(url, "1")
          response.obj.keySet should contain("success")
          response("stdout").str shouldBe "val res0: Int = 1\n"
        }

        "using predef code" in Fixture("val predefCode = 2") { url =>
          val response = postQuerySync(url, "val foo = predefCode + 40")
          response.obj.keySet should contain("success")
          response("stdout").str shouldBe "val foo: Int = 42\n"
        }

        "fail for invalid auth" in Fixture() { url =>
          assertThrows[RequestFailedException] {
            postQuerySync(url, "1", authHeaderVal = "Basic b4df00d")
          }
        }
      }
    }
  }

  private def postQueryAsync(baseUrl: String, query: String, authHeaderVal: String = ValidBasicAuthHeaderVal): Value =
    postQuery(s"$baseUrl/query", query, authHeaderVal)

  private def postQuerySync(baseUrl: String, query: String, authHeaderVal: String = ValidBasicAuthHeaderVal): Value =
    postQuery(s"$baseUrl/query-sync", query, authHeaderVal)

  private def postQuery(endpoint: String, query: String, authHeaderVal: String): Value = {
    val postResponse = requests.post(
      endpoint,
      data = ujson.Obj("query" -> query).toString,
      headers = Seq("authorization" -> authHeaderVal)
    )
    if (postResponse.bytes.length > 0)
      ujson.read(postResponse.bytes)
    else
      ujson.Obj()
  }

  private def getResponse(url: String, uuidParam: String, authHeaderVal: String = ValidBasicAuthHeaderVal): Value = {
    val uri = s"$url/result/${URLEncoder.encode(uuidParam, "utf-8")}"
    val getResponse = requests.get(uri, headers = Seq("authorization" -> authHeaderVal))
    ujson.read(getResponse.bytes)
  }

}

object Fixture {

  def apply[T](predefCode: String = "")(urlToResult: String => T): T = {
    val embeddedRepl = new EmbeddedRepl(predefLines = predefCode.linesIterator)

    val host = "localhost"
    val port = 8081
    val replServer = new ReplServer(embeddedRepl, host, port, Some(UsernamePasswordAuth("username", "password")))
    val server = io.undertow.Undertow.builder
      .addHttpListener(replServer.port, replServer.host)
      .setHandler(replServer.defaultHandler)
      .build
    server.start()
    try {
      urlToResult(s"http://$host:$port")
    } finally {
      server.stop()
      embeddedRepl.shutdown()
    }
  }
}
