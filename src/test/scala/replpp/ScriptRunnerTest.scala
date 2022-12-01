package replpp

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ScriptRunnerTest extends AnyWordSpec with Matchers {

  "execute simple single-statement script" in {
    val scriptFile = os.temp()
    val testOutputFile = os.temp()
    val testOutputPath = testOutputFile.toNIO.toAbsolutePath.toString
    os.write.over(
      scriptFile,
      s"""os.write.over(os.Path("$testOutputPath"), "iwashere")"""
    )

    ScriptRunner.exec(Config(scriptFile = Some(scriptFile)))
    os.read(testOutputFile) shouldBe "iwashere"
  }

//  "@main entry point" in {
//  }

//  "--predefCode" in {
//    ???
//  }

}
