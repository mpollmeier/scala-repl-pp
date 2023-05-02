package replpp

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PredefTests extends AnyWordSpec with Matchers {

  "import predef files in given order" in {
    val predefFile1 = os.temp("val foo = 10").toNIO
    val predefFile2 = os.temp("val bar = foo * 2").toNIO

    allPredefCode(Config(
      predefFiles = Seq(predefFile1, predefFile2),
    )) shouldBe
      """val foo = 10
        |val bar = foo * 2
        |""".stripMargin.trim
  }

  "recursively resolve `//> using file` directive and insert at the top of the referencing file - simple case" in {
    val additionalScript1 = os.temp("val additionalScript1 = 10")
    val additionalScript2 = os.temp("val additionalScript2 = 20")
    val predefFile = os.temp(
      s"""//> using file $additionalScript1
         |val predefCode = 1
         |//> using file $additionalScript2
         |""".stripMargin)

    allPredefCode(Config(predefFiles = Seq(predefFile.toNIO))) shouldBe
      s"""val additionalScript1 = 10
         |val additionalScript2 = 20
         |val predefCode = 1
         |""".stripMargin.trim
  }

  "recursively resolve `//> using file` directive and insert at the top of the referencing file - more complex case" in {
    val additionalScript1 = os.temp("val additionalScript1 = 10")
    val additionalScript2 = os.temp("val additionalScript2 = 20")
    val additionalScript3 = os.temp("val additionalScript3 = 30")
    val additionalScript4 = os.temp(contents =
      s"""//> using file $additionalScript2
         |val additionalScript4 = 40
         |//> using file $additionalScript3
         |""".stripMargin)
    val predefFile = os.temp(
      s"""//> using file $additionalScript1
         |val predefCode = 1
         |//> using file $additionalScript4
         |""".stripMargin)

    allPredefCode(Config(predefFiles = Seq(predefFile.toNIO))) shouldBe
      """val additionalScript1 = 10
        |val additionalScript4 = 40
        |val additionalScript2 = 20
        |val additionalScript3 = 30
        |val predefCode = 1
        |""".stripMargin.trim
  }

  "recursively resolve `//> using file` directive and insert at the top of the referencing file - with recursive loops" in {
    val additionalScript1 = os.temp(suffix = "-script1")
    val additionalScript2 = os.temp(suffix = "-script2")

    os.write.over(additionalScript1,
      s"""//> using file $additionalScript2
         |val additionalScript1 = 10""".stripMargin)
    os.write.over(additionalScript2,
      s"""//> using file $additionalScript1
         |val additionalScript2 = 20
         |""".stripMargin)

    val predefFile = os.temp(
      s"""//> using file $additionalScript1
         |val predefCode = 1
         |""".stripMargin)

    allPredefCode(Config(predefFiles = Seq(predefFile.toNIO))) shouldBe
      // most importantly, this should not loop endlessly due to the recursive imports
      """val additionalScript1 = 10
        |val additionalScript2 = 20
        |val predefCode = 1
        |""".stripMargin.trim
  }
}
