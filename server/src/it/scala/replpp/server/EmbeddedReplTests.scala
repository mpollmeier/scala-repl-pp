package replpp.server

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.concurrent.Semaphore

/** Moved to IntegrationTests, because of some strange interaction with ReplServerTests:
  * if EmbeddedReplTests would run *before* ReplServerTests, the latter would stall (forever)
  * after a few sucessful tests.
  * Run with `sbt IntegrationTest/test`
  */
class EmbeddedReplTests extends AnyWordSpec with Matchers {

  "start and shutdown without hanging" in {
//    val shell = new EmbeddedRepl()
//    shell.start()
//    shell.shutdown()
    ???
  }

  "execute commands synchronously" in {
//    val shell = new EmbeddedRepl()
//    shell.start()
//
//    shell.query("val x = 0").out shouldBe "val x: Int = 0\n"
//    shell.query("x + 1").out     shouldBe "val res1: Int = 1\n"
//
//    shell.shutdown()
    ???
  }

   "execute a command asynchronously" in {
//     val shell = new EmbeddedRepl()
//     val mutex = new Semaphore(0)
//     shell.start()
//     var resultOut = "uninitialized"
//     shell.queryAsync("val x = 0") { result =>
//       resultOut = result.out
//       mutex.release()
//     }
//     mutex.acquire()
//     resultOut shouldBe "val x: Int = 0\n"
//     shell.shutdown()
     ???
   }

}
