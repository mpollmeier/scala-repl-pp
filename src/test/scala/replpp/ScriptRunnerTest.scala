package replpp

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ScriptRunnerTest extends AnyWordSpec with Matchers {

  "execute simple single-statement script" in {
    execTest { testOutputPath =>
      TestSetup(s"""os.write.over(os.Path("$testOutputPath"), "iwashere-simple")""")
    } shouldBe "iwashere-simple"
  }

  "main entry point" in {
    execTest { testOutputPath =>
      TestSetup(
        s"""@main def foo() = {
           |  os.write.over(os.Path("$testOutputPath"), "iwashere-@main")
           |}""".stripMargin
      )
    } shouldBe "iwashere-@main"
  }

  "multiple main entry points" in {
    execTest { testOutputPath =>
      TestSetup(
        s"""@main def foo() = {
           |  os.write.append(os.Path("$testOutputPath"), "iwashere-@main-foo")
           |}
           |
           |@main def bar() = {
           |  os.write.append(os.Path("$testOutputPath"), "iwashere-@main-bar")
           |}""".stripMargin,
        adaptConfig = _.copy(command = Some("bar"))
      )
    } shouldBe "iwashere-@main-bar"
  }

  "parameters" in {
    execTest { testOutputPath =>
      TestSetup(
        s"""@main def foo(value: String) = {
           |  os.write.over(os.Path("$testOutputPath"), value)
           |}""".stripMargin,
        adaptConfig = _.copy(params = Map("value" -> "iwashere-parameter"))
      )
    } shouldBe "iwashere-parameter"
  }

  "--predefCode" in {
    execTest { testOutputPath =>
      TestSetup(
        s"""os.write.over(os.Path("$testOutputPath"), bar)""".stripMargin,
        adaptConfig = _.copy(predefCode = Some("val bar = \"iwashere-predefCode\""))
      )
    } shouldBe "iwashere-predefCode"
  }

  "--predefFiles" in {
    execTest { testOutputPath =>
      val predefFile = os.temp()
      os.write.over(predefFile, "val bar = \"iwashere-predefFile\"")
      TestSetup(
        s"""os.write.over(os.Path("$testOutputPath"), bar)""".stripMargin,
        adaptConfig = _.copy(predefFiles = List(predefFile))
      )
    } shouldBe "iwashere-predefFile"
  }

  "additional dependencies via --dependency" in {
    execTest { testOutputPath =>
      TestSetup(
        s"""val compareResult = versionsort.VersionHelper.compare("1.0", "0.9")
           |os.write.over(os.Path("$testOutputPath"), "iwashere-dependency-param:" + compareResult)
           |""".stripMargin,
        adaptConfig = _.copy(dependencies = Seq("com.michaelpollmeier:versionsort:1.0.7"))
      )
    } shouldBe "iwashere-dependency-param:1"
  }

  "additional dependencies via `//> using lib` in script" in {
    execTest { testOutputPath =>
      TestSetup(
        s"""
           |//> using lib com.michaelpollmeier:versionsort:1.0.7
           |val compareResult = versionsort.VersionHelper.compare("1.0", "0.9")
           |os.write.over(os.Path("$testOutputPath"), "iwashere-dependency-using-script:" + compareResult)
           |""".stripMargin
      )
    } shouldBe "iwashere-dependency-using-script:1"
  }

  "additional dependencies via `//> using lib` in --predefCode" in {
    execTest { testOutputPath =>
      TestSetup(
        s"""
           |val compareResult = versionsort.VersionHelper.compare("1.0", "0.9")
           |os.write.over(os.Path("$testOutputPath"), "iwashere-dependency-using-predefCode:" + compareResult)
           |""".stripMargin,
        adaptConfig = _.copy(predefCode = Some("//> using lib com.michaelpollmeier:versionsort:1.0.7"))
      )
    } shouldBe "iwashere-dependency-using-predefCode:1"
  }

  "additional dependencies via `//> using lib` in --predefFiles" in {
    execTest { testOutputPath =>
      val predefFile = os.temp()
      os.write.over(predefFile, "//> using lib com.michaelpollmeier:versionsort:1.0.7")
      TestSetup(
        s"""
           |val compareResult = versionsort.VersionHelper.compare("1.0", "0.9")
           |os.write.over(os.Path("$testOutputPath"), "iwashere-dependency-using-predefFiles:" + compareResult)
           |""".stripMargin,
        adaptConfig = _.copy(predefFiles = List(predefFile))
      )
    } shouldBe "iwashere-dependency-using-predefFiles:1"
  }

  "import additional files via `//> using file` in main script" in {
    execTest { testOutputPath =>
      val additionalScript = os.temp()
      os.write.over(additionalScript, "def foo = 99")
      TestSetup(
        s"""//> using file $additionalScript
           |os.write.over(os.Path("$testOutputPath"), "iwashere-using-file-test1:" + foo)
           |""".stripMargin
      )
    } shouldBe "iwashere-using-file-test1:99"
  }

  "import additional files via `//> using file` via --predefCode" in {
    execTest { testOutputPath =>
      val additionalScript = os.temp()
      os.write.over(additionalScript, "def foo = 99")
      TestSetup(
        s"""
           |os.write.over(os.Path("$testOutputPath"), "iwashere-using-file-test2:" + foo)
           |""".stripMargin,
        adaptConfig = _.copy(predefCode = Some(s"//> using file $additionalScript"))
      )
    } shouldBe "iwashere-using-file-test2:99"
  }

  "import additional files via `//> using file` via --predefFiles" in {
    execTest { testOutputPath =>
      val additionalScript = os.temp()
      val predefFile = os.temp()
      os.write.over(additionalScript, "def foo = 99")
      os.write.over(predefFile, s"//> using file $additionalScript")
      TestSetup(
        s"""
           |os.write.over(os.Path("$testOutputPath"), "iwashere-using-file-test3:" + foo)
           |""".stripMargin,
        adaptConfig = _.copy(predefFiles = List(predefFile))
      )
    } shouldBe "iwashere-using-file-test3:99"
  }

  "import additional files via `//> using file` recursively" in {
    execTest { testOutputPath =>
      val additionalScript0 = os.temp()
      val additionalScript1 = os.temp()
      // should handle recursive loop as well
      os.write.over(additionalScript0,
        s"""//> using file $additionalScript1
           |def foo = 99""".stripMargin)
      os.write.over(additionalScript1,
        s"""//> using file $additionalScript0
           |def bar = foo""".stripMargin)
      TestSetup(
        s"""//> using file $additionalScript1
           |os.write.over(os.Path("$testOutputPath"), "iwashere-using-file-test4:" + bar)
           |""".stripMargin
      )
    } shouldBe "iwashere-using-file-test4:99"
  }


  type TestOutputPath = String
  type TestOutput = String
  case class TestSetup(scriptSrc: String, adaptConfig: Config => Config = identity)
  private def execTest(setupTest: TestOutputPath => TestSetup): TestOutput = {
    val testOutputFile = os.temp()
    val testOutputPath = testOutputFile.toNIO.toAbsolutePath.toString

    val TestSetup(scriptSrc, adaptConfig) = setupTest(testOutputPath)
    val scriptFile = os.temp()
    os.write.over(scriptFile, scriptSrc)
    val config = adaptConfig(Config(scriptFile = Some(scriptFile)))
    ScriptRunner.exec(config)

    os.read(testOutputFile)
  }

}
