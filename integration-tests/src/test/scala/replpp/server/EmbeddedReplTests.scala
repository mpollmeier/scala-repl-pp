package replpp.server

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/** Moved to integrationTests, because of some strange interaction with ReplServerTests:
  * if EmbeddedReplTests would run *before* ReplServerTests, the latter would stall (forever)
  * after a few sucessful tests.
  * Run with `sbt integrationTest/test`
  */
class EmbeddedReplTests extends AnyWordSpec with Matchers {

  "execute commands synchronously" in {
    val repl = new EmbeddedRepl(defaultCompilerArgs, runBeforeCode = Seq("import Short.MaxValue"))

    repl.query("val x = MaxValue").output.trim shouldBe "val x: Short = 32767"
    repl.query("x + 1").output.trim     shouldBe "val res0: Int = 32768"

    repl.shutdown()
  }

  "execute a command asynchronously" in {
    val repl = new EmbeddedRepl(defaultCompilerArgs, runBeforeCode = Seq("import Short.MaxValue"))
    val (uuid, futureResult) = repl.queryAsync("val x = MaxValue")
    val result = Await.result(futureResult, Duration.Inf)
    result.trim shouldBe "val x: Short = 32767"
    repl.shutdown()
  }

  val defaultCompilerArgs = {
    val inheritedClasspath = System.getProperty("java.class.path")
    Array(
      "-classpath", inheritedClasspath,
      "-explain", // verbose scalac error messages
      "-deprecation",
      "-color", "never"
    )
  }

}
