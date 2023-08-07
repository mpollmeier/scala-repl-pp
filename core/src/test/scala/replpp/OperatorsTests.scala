package replpp.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import replpp.Operators.*

import scala.jdk.CollectionConverters.*
import System.lineSeparator

class OperatorsTests extends AnyWordSpec with Matchers {

  "#> redirects output into a file, overriding it" when {
    "using on String" in {
      val tmpFile = os.temp("old")
      "new" #> tmpFile.toString
      os.read(tmpFile) shouldBe "new" + lineSeparator
    }
    "using on IterableOnce" in {
      val tmpFile = os.temp("old")
      val values: IterableOnce[String] = Seq("new1", "new2")
      values #> tmpFile.toString
      os.read.lines(tmpFile) shouldBe values
    }
    "using on java Iterable" in {
      val tmpFile = os.temp("old")
      val values: java.lang.Iterable[String] = Seq("new1", "new2").asJava
      values #> tmpFile.toString
      os.read.lines(tmpFile) shouldBe values.asScala.toSeq
    }
  }

  "#>> redirects output into a file, appending to it" when {
    "using on String" in {
      val tmpFile = os.temp()
      "aaa" #>> tmpFile.toString
      "bbb" #>> tmpFile.toString
      os.read.lines(tmpFile) shouldBe Seq("aaa", "bbb")
    }
    "using on IterableOnce" in {
      val tmpFile = os.temp()
      val values1: IterableOnce[String] = Seq("aaa", "bbb")
      values1 #>> tmpFile.toString
      Seq("ccc", "ddd") #>> tmpFile.toString
      os.read.lines(tmpFile) shouldBe Seq("aaa", "bbb", "ccc", "ddd")
    }
    "using on java Iterable" in {
      val tmpFile = os.temp()
      val values: java.lang.Iterable[String] = Seq("aaa", "bbb").asJava
      values #>> tmpFile.toString
      Seq("ccc", "ddd").asJava #>> tmpFile.toString
      os.read.lines(tmpFile) shouldBe Seq("aaa", "bbb", "ccc", "ddd")
    }
  }

  "#| pipes into an external command" when {
      if (scala.util.Properties.isWin) {
        info("#| not unit-tested in windows")
        // TODO implmement
        ???
      } else {
        "using on String" in {
          val value = "aaa"
          val result = value #| "cat"
          result shouldBe value
        }
        "using on IterableOnce" in {
          val values: IterableOnce[String] = Seq("aaa", "bbb")
          val result = values #| "cat"
          result shouldBe """aaa
                            |bbb""".stripMargin
        }
        "using on java Iterable" in {
          val values: java.lang.Iterable[String] = Seq("aaa", "bbb").asJava
          val result = values #| "cat"
          result shouldBe """aaa
                            |bbb""".stripMargin
        }
      }
  }
}
