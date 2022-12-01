package replpp

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ScriptRunnerTest extends AnyWordSpec with Matchers {

  "execute simple single-statement script" in {
    val testOutputFile = os.temp()
    val testOutputPath = testOutputFile.toNIO.toAbsolutePath.toString

    exec(s"""os.write.over(os.Path("$testOutputPath"), "iwashere")""")

    os.read(testOutputFile) shouldBe "iwashere"
  }

//  "@main entry point" in {
//  }

//  "--predefCode" in {
//    ???
//  }

  private def exec(scriptSrc: String): Unit = {
    val scriptFile = os.temp()
    os.write.over(scriptFile, scriptSrc)
    ScriptRunner.exec(Config(scriptFile = Some(scriptFile)))
  }


}
