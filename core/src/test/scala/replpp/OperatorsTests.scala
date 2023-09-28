package replpp.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import replpp.Colors
import replpp.Operators.*

import scala.jdk.CollectionConverters.*
import System.lineSeparator

/* note: `inheritIO` mode can only be tested manually: it's supposed to open `less` in the terminal with the given input
  ```
  "this is a test" #|^ "less"

  Seq("this is a test", "another one") #|^ "less"

  import scala.jdk.CollectionConverters.*
  Seq("this is a test", "another one").asJava #|^ "less"
  ```
*/
class OperatorsTests extends AnyWordSpec with Matchers {
  given Colors = Colors.BlackWhite

  case class PrettyPrintable(s: String, i: Int)

  "#> and #>> override and append to file" when {
    "using single objects" in {
      val result = withTempFile { path =>
        "foo" #> path
        PrettyPrintable("two", 2) #>> path
      }
      result shouldBe
        """foo
          |PrettyPrintable(s = "two", i = 2)
          |""".stripMargin

      // double checking that it ends with a lineSeparator
      result shouldBe "foo" + lineSeparator + """PrettyPrintable(s = "two", i = 2)""" + lineSeparator
    }

    "using IterableOnce" in {
      val values: IterableOnce[_] = Seq("foo", PrettyPrintable("two", 2))
      withTempFile { path =>
        values #> path
        "-----" #>> path
        values #>> path
      } shouldBe
        """foo
          |PrettyPrintable(s = "two", i = 2)
          |-----
          |foo
          |PrettyPrintable(s = "two", i = 2)
          |""".stripMargin
    }

    "using java.lang.Iterable" in {
      val values: java.lang.Iterable[_] = Seq("foo", PrettyPrintable("two", 2)).asJava
      withTempFile { path =>
        values #> path
        "-----" #>> path
        values #>> path
      } shouldBe
        """foo
          |PrettyPrintable(s = "two", i = 2)
          |-----
          |foo
          |PrettyPrintable(s = "two", i = 2)
          |""".stripMargin
    }

    "using Array" in {
      val values: Array[_] = Seq("foo", PrettyPrintable("two", 2)).toArray
      withTempFile { path =>
        values #> path
        "-----" #>> path
        values #>> path
      } shouldBe
        """foo
          |PrettyPrintable(s = "two", i = 2)
          |-----
          |foo
          |PrettyPrintable(s = "two", i = 2)
          |""".stripMargin
    }

    "using Iterator" in {
      def values: Iterator[_] = Seq("foo", PrettyPrintable("two", 2)).iterator
      withTempFile { path =>
        values #> path
        "-----" #>> path
        values #>> path
      } shouldBe
        """foo
          |PrettyPrintable(s = "two", i = 2)
          |-----
          |foo
          |PrettyPrintable(s = "two", i = 2)
          |""".stripMargin
    }

    "using java.util.Iterator" in {
      def values: java.util.Iterator[_] = Seq("foo", PrettyPrintable("two", 2)).asJava.iterator()
      withTempFile { path =>
        values #> path
        "-----" #>> path
        values #>> path
      } shouldBe
        """foo
          |PrettyPrintable(s = "two", i = 2)
          |-----
          |foo
          |PrettyPrintable(s = "two", i = 2)
          |""".stripMargin
    }
  }

  "#| pipes into an external command" when {
    if (scala.util.Properties.isWin) {
      info("#| is not unit-tested yet on windows - no idea what the equivalent of `cat` is")
    } else {
      "using String" in {
        val value = "foo"
        val result = value #| "cat"
        result shouldBe value
      }

      "using case class" in {
        val result = PrettyPrintable("two", 2) #| "cat"
        result shouldBe """PrettyPrintable(s = "two", i = 2)"""
      }

      "using list types" when {
        val values = Seq("foo", PrettyPrintable("two", 2))
        Seq(
          ("IterableOnce", values: IterableOnce[_]),
          ("java.lang.Iterable", values.asJava: java.lang.Iterable[_]),
          ("Array", values.toArray: Array[_]),
          ("Iterator", values.iterator: Iterator[_]),
          ("java.util.Iterator", values.asJava.iterator: java.util.Iterator[_]),
        ).foreach { case (listType, list) =>
          listType in {
            val result = list #| "cat"
            result shouldBe
              """foo
                |PrettyPrintable(s = "two", i = 2)""".stripMargin
          }
        }
      }

      "passing arguments to the external command" in {
        val result1 = Seq("foo", "bar", "foobar") #| ("grep", "foo")
        result1 shouldBe """foo
                          |foobar""".stripMargin


        val input2 = """foo
                       |bar
                       |foobar""".stripMargin
        val result2 = input2 #| ("grep", "foo")
        result2 shouldBe
          """foo
            |foobar""".stripMargin
      }
    }
  }

  "unwrapping of list-types should only happen at root level" should {
    "happen at root level" in {
      withTempFile { file =>
        Seq(
          "one",
          Seq("two"),
          Seq("three", 4),
          5
        ) #> file
      } shouldBe
        """one
          |List("two")
          |List("three", 4)
          |5
          |""".stripMargin
    }
  }

  def withTempFile(funWithPath: String => Unit): String = {
    val tmpFile = os.temp(contents = "initial contents", prefix = getClass.getName)
    try {
      funWithPath(tmpFile.toString)
      os.read(tmpFile)
    } finally {
      os.remove(tmpFile)
    }
  }

}