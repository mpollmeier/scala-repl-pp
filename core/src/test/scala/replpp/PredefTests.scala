package replpp

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PredefTests extends AnyWordSpec with Matchers {
  given Colors = Colors.BlackWhite

  "recursively resolve `//> using file` directive" in {
    val script = os.temp("val mainScript = 5")
    val additionalScript1 = os.temp("val additionalScript1 = 10")
    val additionalScript2 = os.temp("val additionalScript2 = 20")
    val predefFile = os.temp(
      s"""//> using file $additionalScript1
         |val predefCode = 1
         |//> using file $additionalScript2
         |""".stripMargin)

    allSourceLines(Config(predefFiles = Seq(predefFile.toNIO), scriptFile = Some(script.toNIO))).sorted shouldBe
      Seq(
        s"//> using file $additionalScript1",
        s"//> using file $additionalScript2",
        "val additionalScript1 = 10",
        "val additionalScript2 = 20",
        "val mainScript = 5",
        "val predefCode = 1",
      ).sorted
  }

  "recursively resolve `//> using file` directive - with recursive loops" in {
    val script = os.temp("val mainScript = 5")
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

    // most importantly, this should not loop endlessly due to the recursive imports
    allSourceLines(Config(predefFiles = Seq(predefFile.toNIO), scriptFile = Some(script.toNIO))).distinct.sorted shouldBe
      Seq(
        s"//> using file $additionalScript1",
        s"//> using file $additionalScript2",
        "val additionalScript1 = 10",
        "val additionalScript2 = 20",
        "val mainScript = 5",
        "val predefCode = 1",
      ).sorted
  }
}
