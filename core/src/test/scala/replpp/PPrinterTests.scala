package replpp

import java.nio.file.Files
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import replpp.shaded.fansi
import replpp.util.ProjectRoot
import scala.util.Try

/** We use source-highlight to encode source as ansi strings, e.g. the .dump step Ammonite uses fansi for it's
  * colour-coding, and while both pledge to follow the ansi codec, they aren't compatible TODO: PR for fansi to support
  * these standard encodings out of the box
  */
class PPrinterTests extends AnyWordSpec with Matchers {

  "print some common datastructures" in {
    testRendering(
      List(1, 2),
      expectedUncolored = "List(1, 2)",
      expectedColored = "<Y|List>(<G|1>, <G|2>)"
    )

    testRendering(
      (1,2,"three"),
      expectedUncolored = """(1, 2, "three")""",
      expectedColored = """(<G|1>, <G|2>, <G|"three">)"""
    )

    case class A(i: Int, s: String)
    testRendering(
      A(42, "foo bar"),
      expectedUncolored = """A(i = 42, s = "foo bar")""",
      expectedColored = """<Y|A>(i = <G|42>, s = <G|"foo bar">)"""
    )

    val productWithLabels = new Product {
      override def productPrefix = "Foo"

      def productArity = 2

      override def productElementName(n: Int) =
        n match {
          case 0 => "first"
          case 1 => "second"
        }

      def productElement(n: Int) = n match {
        case 0 => "one"
        case 1 => "two"
      }

      def canEqual(that: Any): Boolean = ???
    }

    testRendering(
      productWithLabels,
      expectedUncolored = """Foo(first = "one", second = "two")""",
      expectedColored = """<Y|Foo>(first = <G|"one">, second = <G|"two">)"""
    )
  }

  "render strings with string escapes using triple quotes" in {
    PPrinter("""a\b""", nocolors = true) shouldBe "     \"\"\"a\\b\"\"\"    ".trim
  }

  "don't error on invalid ansi encodings" in {
    val invalidAnsi = Files.readString(ProjectRoot.relativise("core/src/test/resources/invalid-ansi.txt"))
    Try {
      PPrinter(invalidAnsi, nocolors = true)
      PPrinter(invalidAnsi)
    }.isSuccess shouldBe true
  }

  "fansi encoding fix" must {
    "handle different ansi encoding termination" in {
      // encoding ends with [39m for fansi instead of [m
      val fixedForFansi = PPrinter.fixForFansi(IntGreenForeground)
      fixedForFansi shouldBe fansi.Str("\u001b[32mint\u001b[39m")
      fansi.Str(fixedForFansi) shouldBe fansi.Color.Green("int")
    }

    "handle different single-digit encodings" in {
      // `[01m` is encoded as `[1m` in fansi for all single digit numbers
      val fixedForFansi = PPrinter.fixForFansi(FBold)
      fixedForFansi shouldBe fansi.Str("\u001b[1mF\u001b[39m")
      fansi.Str(fixedForFansi) shouldBe fansi.Str("F").overlay(fansi.Bold.On)
    }

    "handle multi-encoded parts" in {
      // `[01;34m` is encoded as `[1m[34m` in fansi
      val fixedForFansi = PPrinter.fixForFansi(IfBlueBold)
      fixedForFansi shouldBe fansi.Str("\u001b[1m\u001b[34mif\u001b[39m")
      fansi.Str(fixedForFansi) shouldBe fansi.Color.Blue("if").overlay(fansi.Bold.On)
    }

    "handle 8bit (256) 'full' colors" in {
      // `[00;38;05;70m` is encoded as `[38;5;70m` in fansi
      val fixedForFansi = PPrinter.fixForFansi(X8bit)
      fixedForFansi shouldBe fansi.Str("\u001b[38;5;70mX\u001b[39m")
      fansi.Str(fixedForFansi) shouldBe fansi.Color.Full(70)("X")
    }

    // ansi colour-encoded strings as source-highlight produces them
    lazy val IntGreenForeground = "\u001b[32mint\u001b[m"
    lazy val IfBlueBold = "\u001b[01;34mif\u001b[m"
    lazy val FBold = "\u001b[01mF\u001b[m"
    lazy val X8bit = "\u001b[00;38;05;70mX\u001b[m"
  }

  def testRendering(value: AnyRef, expectedUncolored: String, expectedColored: String): Unit = {
    PPrinter(value, nocolors = true) shouldBe expectedUncolored

    val resultColored = PPrinter(value)
    replaceColorEncodingForTest(resultColored) shouldBe expectedColored
  }

  // adapted from dotty SyntaxHighlightingTests
  private def replaceColorEncodingForTest(s: String): String = {
    val res = s
      .replace(Console.RESET, ">")
      .replace(fansi.Color.Reset.escape, ">")
      .replace(Console.YELLOW, "<Y|")
      .replace(Console.RED, "<R|")
      .replace(Console.GREEN, "<G|")
    res
  }
}
