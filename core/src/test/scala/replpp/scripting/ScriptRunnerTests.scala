package replpp.scripting

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import replpp.Config
import scala.util.{Failure, Success, Try}

class ScriptRunnerTests extends AnyWordSpec with Matchers {

  if (scala.util.Properties.isWin) {
    info("tests were cancelled currently fail on windows - not sure why - I'm testing the `--script` mode manually for now and will replace these tests in the future")
  } else {
    "execute simple single-statement script" in {
      execTest { testOutputPath =>
        TestSetup(s"""java.nio.file.Files.writeString(java.nio.file.Path.of(\"\"\"$testOutputPath\"\"\"), "iwashere-simple")""")
      }.get shouldBe "iwashere-simple"
    }

    "main entry point" in {
      execTest { testOutputPath =>
        TestSetup(
          s"""import java.nio.file.*
             |@main def foo() = {
             |  Files.writeString(Path.of(\"\"\"$testOutputPath\"\"\"), "iwashere-@main")
             |}""".stripMargin
        )
      }.get shouldBe "iwashere-@main"
    }

    "multiple main entry points" in {
      execTest { testOutputPath =>
        TestSetup(
          s"""import java.nio.file.*
             |@main def foo() = {
             |  Files.writeString(Path.of(\"\"\"$testOutputPath\"\"\"), "iwashere-@main-foo")
             |}
             |
             |@main def bar() = {
             |  Files.writeString(Path.of(\"\"\"$testOutputPath\"\"\"), "iwashere-@main-bar")
             |}""".stripMargin,
          adaptConfig = _.copy(command = Some("bar"))
        )
      }.get shouldBe "iwashere-@main-bar"
    }

    "parameters" in {
      execTest { testOutputPath =>
        TestSetup(
          s"""import java.nio.file.*
             |@main def foo(value: String) = {
             |  Files.writeString(Path.of(\"\"\"$testOutputPath\"\"\"), value)
             |}""".stripMargin,
          adaptConfig = _.copy(params = Map("value" -> "iwashere-parameter"))
        )
      }.get shouldBe "iwashere-parameter"
    }

    "predefFiles" in {
      execTest { testOutputPath =>
        val predefFile = os.temp("val bar = \"iwashere-predefFile\"").toNIO
        TestSetup(
          s"""import java.nio.file.*
             |Files.writeString(Path.of(\"\"\"$testOutputPath\"\"\"), bar)""".stripMargin,
          adaptConfig = _.copy(predefFiles = List(predefFile))
        )
      }.get shouldBe "iwashere-predefFile"
    }

    "predefFiles imports are available" in {
      execTest { testOutputPath =>
        val predefFile = os.temp("import Byte.MaxValue").toNIO
        TestSetup(
          s"""import java.nio.file.*
             |Files.writeString(Path.of(\"\"\"$testOutputPath\"\"\"), "iwashere-predefFile-" + MaxValue)""".stripMargin,
          adaptConfig = _.copy(predefFiles = List(predefFile))
        )
      }.get shouldBe "iwashere-predefFile-127"
    }

    "additional dependencies" in {
      execTest { testOutputPath =>
        TestSetup(
          s"""import java.nio.file.*
             |val compareResult = versionsort.VersionHelper.compare("1.0", "0.9")
             |Files.writeString(Path.of(\"\"\"$testOutputPath\"\"\"), "iwashere-dependency-param:" + compareResult)
             |""".stripMargin,
          adaptConfig = _.copy(classpathConfig = Config.ForClasspath(dependencies = Seq("com.michaelpollmeier:versionsort:1.0.7")))
        )
      }.get shouldBe "iwashere-dependency-param:1"
    }

    "additional dependencies via `//> using dep` in script" in {
      execTest { testOutputPath =>
        TestSetup(
          s"""import java.nio.file.*
             |//> using dep com.michaelpollmeier:versionsort:1.0.7
             |val compareResult = versionsort.VersionHelper.compare("1.0", "0.9")
             |Files.writeString(Path.of(\"\"\"$testOutputPath\"\"\"), "iwashere-dependency-using-script:" + compareResult)
             |""".stripMargin
        )
      }.get shouldBe "iwashere-dependency-using-script:1"
    }

    "additional dependencies via `//> using dep` in predefFiles" in {
      execTest { testOutputPath =>
        val predefFile = os.temp("//> using dep com.michaelpollmeier:versionsort:1.0.7").toNIO
        TestSetup(
          s"""import java.nio.file.*
             |val compareResult = versionsort.VersionHelper.compare("1.0", "0.9")
             |Files.writeString(Path.of(\"\"\"$testOutputPath\"\"\"), "iwashere-dependency-using-predefFiles:" + compareResult)
             |""".stripMargin,
          adaptConfig = _.copy(predefFiles = List(predefFile))
        )
      }.get shouldBe "iwashere-dependency-using-predefFiles:1"
    }

    "import additional files via `//> using file` in main script" in {
      execTest { testOutputPath =>
        val additionalScript = os.temp()
        os.write.over(additionalScript, "def foo = 99")
        TestSetup(
          s"""import java.nio.file.*
             |//> using file $additionalScript
             |Files.writeString(Path.of(\"\"\"$testOutputPath\"\"\"), "iwashere-using-file-test1:" + foo)
             |""".stripMargin
        )
      }.get shouldBe "iwashere-using-file-test1:99"
    }

    "import additional files via `//> using file` via predefFiles" in {
      execTest { testOutputPath =>
        val additionalScript = os.temp("def foo = 99")
        val predefFile = os.temp(s"//> using file $additionalScript").toNIO
        TestSetup(
          s"""import java.nio.file.*
             |Files.writeString(Path.of(\"\"\"$testOutputPath\"\"\"), "iwashere-using-file-test3:" + foo)
             |""".stripMargin,
          adaptConfig = _.copy(predefFiles = List(predefFile))
        )
      }.get shouldBe "iwashere-using-file-test3:99"
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
             |import java.nio.file.*
             |Files.writeString(Path.of(\"\"\"$testOutputPath\"\"\"), "iwashere-using-file-test4:" + bar)
             |""".stripMargin
        )
      }.get shouldBe "iwashere-using-file-test4:99"
    }

    "import additional files: use relative path of dependent script, and import in correct order" in {
      execTest { testOutputPath =>
        val scriptRootDir = os.temp.dir()
        val scriptSubDir = scriptRootDir / "subdir"
        os.makeDir(scriptSubDir)
        val additionalScript0 = scriptRootDir / "additional-script0.sc"
        val additionalScript1 = scriptSubDir / "additional-script1.sc"
        os.write.over(additionalScript0,
          // specifying path relative from the additionalScript0
          s"""//> using file subdir/additional-script1.sc
             |val bar = foo""".stripMargin)
        os.write.over(additionalScript1,
          s"""def foo = 99""".stripMargin)
        TestSetup(
          s"""//> using file $additionalScript0
             |import java.nio.file.*
             |Files.writeString(Path.of(\"\"\"$testOutputPath\"\"\"), "iwashere-using-file-test5:" + bar)
             |""".stripMargin,
          adaptConfig = _.copy(verbose = true)
        )
      }.get shouldBe "iwashere-using-file-test5:99"
    }

    "fail on compilation error" when {
      "error is in main script" in {
        ensureErrors { () =>
          TestSetup(
            s"""val thisWillNotCompile: Int = "because we need an Int""""
          )
        }
        // TODO: this isn't the case yet: note: if we intercepted the stdout/stderr, we could/should observe that the error is reported in line 1
      }

      "error is in imported file" in {
        ensureErrors { () =>
          val additionalScript = os.temp()
          os.write.over(additionalScript,
            s"""val foo = 42
               |val thisWillNotCompile: Int = "because we need an Int"
               |""".stripMargin)
          TestSetup(
            s"""//> using file $additionalScript
               |val bar = 34
               |""".stripMargin
          )
        }
        // note: if we intercepted the stdout/stderr, we could/should observe that the error is reported in line 2
      }

      "error is in predef file" in {
        ensureErrors { () =>
          val predefFile = os.temp()
          os.write.over(predefFile,
            s"""val foo = 42
               |val thisWillNotCompile: Int = "because we need an Int"
               |""".stripMargin)
          TestSetup(
            "val bar = 34".stripMargin,
            adaptConfig = _.copy(predefFiles = Seq(predefFile.toNIO))
          )
        }
        // note: if we intercepted the stdout/stderr, we could/should observe that the error is reported in line 2
      }

    }
  }

  type TestOutputPath = String
  type TestOutput = String
  case class TestSetup(scriptSrc: String, adaptConfig: Config => Config = identity)

  private def execTest(setupTest: TestOutputPath => TestSetup): Try[TestOutput] = {
    val testOutputFile = os.temp(deleteOnExit = false)
    val testOutputPath = replpp.util.pathAsString(testOutputFile.toNIO)

    val TestSetup(scriptSrc, adaptConfig) = setupTest(testOutputPath)
    val scriptFile = os.temp(scriptSrc).toNIO
    val config = adaptConfig(Config(scriptFile = Some(scriptFile), verbose = false))
    ScriptRunner.exec(config).map { _ => os.read(testOutputFile) }
  }

  private def ensureErrors(fun: () => TestSetup): Unit = {
    execTest(_ => fun()) match {
      case Success(_) => fail("the script was supposed to fail, but it succeeded...")
      case Failure(exception) => exception.getMessage should include("exit code was 1")
    }
  }
}

/** for manual testing */
object ScriptRunnerTests {
  def main(args: Array[String]): Unit = {
    val scriptSrc =
      s"""val i = 2 + 10
         |println("in main/script: i=" + i)
         |""".stripMargin
    val scriptFile = os.temp(scriptSrc).toNIO
    val config = Config(scriptFile = Some(scriptFile), verbose = true)
    ScriptRunner.exec(config)
  }
}

