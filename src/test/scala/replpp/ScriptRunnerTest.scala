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

  "--predefCode" in {
    val testOutputFile = os.temp()
    val testOutputPath = testOutputFile.toNIO.toAbsolutePath.toString
    exec(
      s"""@main def foo() = {
         |  os.write.over(os.Path("$testOutputPath"), bar)
         |}""".stripMargin,
        adaptConfig = _.copy(predefCode = Some("val bar = \"iwashere-predefCode\""))
    )

    os.read(testOutputFile) shouldBe "iwashere-predefCode"
  }

  private def exec(scriptSrc: String, adaptConfig: Config => Config = identity): Unit = {
    val scriptFile = os.temp()
    os.write.over(scriptFile, scriptSrc)
    val config = adaptConfig(Config(scriptFile = Some(scriptFile)))
    ScriptRunner.exec(config)
  }


}
