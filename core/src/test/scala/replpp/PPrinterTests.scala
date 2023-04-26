package replpp

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** We use source-highlight to encode source as ansi strings, e.g. the .dump step Ammonite uses fansi for it's
  * colour-coding, and while both pledge to follow the ansi codec, they aren't compatible TODO: PR for fansi to support
  * these standard encodings out of the box
  */
class PPrinterTests extends AnyWordSpec with Matchers {
  // ansi colour-encoded strings as source-highlight produces them
  val IntGreenForeground = "\u001b[32mint\u001b[m"
  val IfBlueBold         = "\u001b[01;34mif\u001b[m"
  val FBold              = "\u001b[01mF\u001b[m"
  val X8bit              = "\u001b[00;38;05;70mX\u001b[m"

  "print some common datastructures" in {
    PPrinter(List(1,2)) shouldBe "List(1, 2)"
    PPrinter((1,2,"three"))  shouldBe """(1, 2, "three")"""

    case class A(i: Int, s: String)
    PPrinter(A(42, "foo bar"))  shouldBe """A(i = 42, s = "foo bar")"""

    PPrinter(new Product {
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
    }) shouldBe """Foo(first = "one", second = "two")"""
  }

  "fansi encoding fix" must {
    "handle different ansi encoding termination" in {
      // encoding ends with [39m for fansi instead of [m
      val fixedForFansi = PPrinter.fixForFansi(IntGreenForeground)
      fixedForFansi shouldBe "\u001b[32mint\u001b[39m"
      fansi.Str(fixedForFansi) shouldBe fansi.Color.Green("int")
    }

    "handle different single-digit encodings" in {
      // `[01m` is encoded as `[1m` in fansi for all single digit numbers
      val fixedForFansi = PPrinter.fixForFansi(FBold)
      fixedForFansi shouldBe "\u001b[1mF\u001b[39m"
      fansi.Str(fixedForFansi) shouldBe fansi.Str("F").overlay(fansi.Bold.On)
    }

    "handle multi-encoded parts" in {
      // `[01;34m` is encoded as `[1m[34m` in fansi
      val fixedForFansi = PPrinter.fixForFansi(IfBlueBold)
      fixedForFansi shouldBe "\u001b[1m\u001b[34mif\u001b[39m"
      fansi.Str(fixedForFansi) shouldBe fansi.Color.Blue("if").overlay(fansi.Bold.On)
    }

    "handle 8bit (256) 'full' colors" in {
      // `[00;38;05;70m` is encoded as `[38;5;70m` in fansi
      val fixedForFansi = PPrinter.fixForFansi(X8bit)
      fixedForFansi shouldBe "\u001b[38;5;70mX\u001b[39m"
      fansi.Str(fixedForFansi) shouldBe fansi.Color.Full(70)("X")
    }
  }

}
