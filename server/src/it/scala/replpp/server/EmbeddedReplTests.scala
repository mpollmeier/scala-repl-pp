package replpp.server

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/** Moved to IntegrationTests, because of some strange interaction with ReplServerTests:
  * if EmbeddedReplTests would run *before* ReplServerTests, the latter would stall (forever)
  * after a few sucessful tests.
  * Run with `sbt IntegrationTest/test`
  */
class EmbeddedReplTests extends AnyWordSpec with Matchers {

  "execute commands synchronously" in {
    val repl = new EmbeddedRepl()

    repl.query("val x = 0").output.trim shouldBe "val x: Int = 0"
    repl.query("x + 1").output.trim     shouldBe "val res0: Int = 1"

    repl.shutdown()
  }

  "execute a command asynchronously" in {
    val repl = new EmbeddedRepl()
    val (uuid, futureResult) = repl.queryAsync("val x = 0")
    val result = Await.result(futureResult, Duration.Inf)
    result.trim shouldBe "val x: Int = 0"
    repl.shutdown()
  }

}
