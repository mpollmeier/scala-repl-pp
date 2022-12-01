package replpp

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ScriptRunnerTest extends AnyWordSpec with Matchers {

  "execute simple single-statement script" in {
    val testOutputFile = os.temp()
    val testOutputPath = testOutputFile.toNIO.toAbsolutePath.toString
    exec(s"""os.write.over(os.Path("$testOutputPath"), "iwashere-simple")""")
    os.read(testOutputFile) shouldBe "iwashere-simple"
  }

  "main entry point" in {
    val testOutputFile = os.temp()
    val testOutputPath = testOutputFile.toNIO.toAbsolutePath.toString
    exec(
      s"""@main def foo() = {
         |  os.write.over(os.Path("$testOutputPath"), "iwashere-@main")
         |}""".stripMargin)

    os.read(testOutputFile) shouldBe "iwashere-@main"
  }

//  "--predefCode" in {
//    ???
//  }

  private def exec(scriptSrc: String): Unit = {
    val scriptFile = os.temp()
    os.write.over(scriptFile, scriptSrc)
    ScriptRunner.exec(Config(scriptFile = Some(scriptFile)))
  }


}
