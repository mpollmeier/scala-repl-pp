package replpp.util

import scala.jdk.CollectionConverters.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import replpp.util.Pipes.*

class PipesTests extends AnyWordSpec with Matchers {

  "#> pipes output into a file, overriding it" when {
    "using on String" in {
      val tmpFile = os.temp("old")
      "new" #> tmpFile.toString
      os.read(tmpFile) shouldBe "new"
    }
    "using on IterableOnce" in {
      val tmpFile = os.temp("old")
      val values: IterableOnce[String] = Seq("new1", "new2")
      values #> tmpFile.toString
      os.read.lines(tmpFile) shouldBe values
    }
    "using on java Iterator" in {
      val tmpFile = os.temp("old")
      val values: java.lang.Iterable[String] = Seq("new1", "new2").asJava
      values #> tmpFile.toString
      os.read.lines(tmpFile) shouldBe values.asScala.toSeq
    }
  }

  "#>> pipes output into a file, appending to it" when {
    "using on String" in {
      val tmpFile = os.temp("old")
      "new" #>> tmpFile.toString
      os.read.lines(tmpFile) shouldBe Seq("old", "new")
    }
    "using on IterableOnce" in {
      val tmpFile = os.temp("old")
      val values: IterableOnce[String] = Seq("new1", "new2")
      values #>> tmpFile.toString
      os.read.lines(tmpFile) shouldBe Seq("old", "new1", "new2")
    }
    "using on java Iterator" in {
      val tmpFile = os.temp("old")
      val values: java.lang.Iterable[String] = Seq("new1", "new2").asJava
      values #>> tmpFile.toString
      os.read.lines(tmpFile) shouldBe Seq("old", "new1", "new2")
    }
  }

}
