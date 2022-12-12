package replpp.server

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.concurrent.Semaphore

/** Marked all tests as ignore, because of some strange interaction with ReplServerTests:
  * if EmbeddedReplTests would run *before* ReplServerTests, the latter would stall (forever)
  * after a few sucessful tests. Since the EmbeddedReplTests aren't as critical, we're
  * marking them as 'ignore' and can still run manually, or in isolation.
  * An alternative solution would be to move them to a separate test project - one would have
  * to ensure they're running at the very end... 
  */
class EmbeddedReplTests extends AnyWordSpec with Matchers {

  "start and shutdown without hanging" ignore {
    val shell = new EmbeddedRepl()
    shell.start()
    shell.shutdown()
  }

  "execute commands synchronously" ignore {
    val shell = new EmbeddedRepl()
    shell.start()

    shell.query("val x = 0").out shouldBe "val x: Int = 0\n"
    shell.query("x + 1").out     shouldBe "val res1: Int = 1\n"

    shell.shutdown()
  }

   "execute a command asynchronously" ignore {
     val shell = new EmbeddedRepl()
     val mutex = new Semaphore(0)
     shell.start()
     var resultOut = "uninitialized"
     shell.queryAsync("val x = 0") { result =>
       resultOut = result.out
       mutex.release()
     }
     mutex.acquire()
     resultOut shouldBe "val x: Int = 0\n"
     shell.shutdown()
   }

}
