package replpp

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ScriptRunnerTest extends AnyWordSpec with Matchers {

  "execute simple single-statement script" in {
    val (testOutputFile, testOutputPath) = newTempFileWithPath
    exec(s"""os.write.over(os.Path("$testOutputPath"), "iwashere-simple")""")
    os.read(testOutputFile) shouldBe "iwashere-simple"
  }

  "main entry point" in {
    val (testOutputFile, testOutputPath) = newTempFileWithPath
    exec(
      s"""@main def foo() = {
         |  os.write.over(os.Path("$testOutputPath"), "iwashere-@main")
         |}""".stripMargin)

    os.read(testOutputFile) shouldBe "iwashere-@main"
  }

  "multiple main entry points" in {
    val (testOutputFile, testOutputPath) = newTempFileWithPath
    exec(
      s"""@main def foo() = {
         |  os.write.append(os.Path("$testOutputPath"), "iwashere-@main-foo")
         |}
         |
         |@main def bar() = {
         |  os.write.append(os.Path("$testOutputPath"), "iwashere-@main-bar")
         |}""".stripMargin,
      adaptConfig = _.copy(command = Some("bar"))
    )

    os.read(testOutputFile) shouldBe "iwashere-@main-bar"
  }

  "parameters" in {
    val (testOutputFile, testOutputPath) = newTempFileWithPath
    exec(
      s"""@main def foo(value: String) = {
         |  os.write.over(os.Path("$testOutputPath"), value)
         |}""".stripMargin,
      adaptConfig = _.copy(params = Map("value" -> "iwashere-parameter"))
    )

    os.read(testOutputFile) shouldBe "iwashere-parameter"
  }

  "--predefCode" in {
    val (testOutputFile, testOutputPath) = newTempFileWithPath
    exec(
      s"""@main def foo() = {
         |  os.write.over(os.Path("$testOutputPath"), bar)
         |}""".stripMargin,
      adaptConfig = _.copy(predefCode = Some("val bar = \"iwashere-predefCode\""))
    )

    os.read(testOutputFile) shouldBe "iwashere-predefCode"
  }

  "--predefFiles" in {
    val (testOutputFile, testOutputPath) = newTempFileWithPath
    val predefFile = os.temp()
    os.write.over(predefFile, "val bar = \"iwashere-predefFile\"")
    exec(
      s"""@main def foo() = {
         |  os.write.over(os.Path("$testOutputPath"), bar)
         |}""".stripMargin,
        adaptConfig = _.copy(predefFiles = List(predefFile))
    )

    os.read(testOutputFile) shouldBe "iwashere-predefFile"
  }

  private def exec(scriptSrc: String, adaptConfig: Config => Config = identity): Unit = {
    val scriptFile = os.temp()
    os.write.over(scriptFile, scriptSrc)
    val config = adaptConfig(Config(scriptFile = Some(scriptFile)))
    ScriptRunner.exec(config)
  }

  private def newTempFileWithPath: (os.Path, String) = {
    val tmpFile = os.temp()
    (tmpFile, tmpFile.toNIO.toAbsolutePath.toString)
  }

}
