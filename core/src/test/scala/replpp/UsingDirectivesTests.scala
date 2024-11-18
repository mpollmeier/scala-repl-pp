package replpp

import java.nio.file.{Path, Paths}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.jdk.CollectionConverters.*

class UsingDirectivesTests extends AnyWordSpec with Matchers {

  "find imported files in given source" in {
    val rootPath = Paths.get(".")
    val source =
      """
        |//> using file simple.sc
        |//> using file /path/to/absolute.sc
        |//> using file ../path/to/relative.sc
        |// //> using file commented_out.sc
        |""".stripMargin

    val results = UsingDirectives.findImportedFiles(source.lines().iterator().asScala, rootPath)
    results should contain(Paths.get("./simple.sc"))
    results should contain(Paths.get("/path/to/absolute.sc"))
    results should contain(Paths.get("./../path/to/relative.sc"))
    results should not contain Paths.get("./commented_out.sc")
  }

  "recursively resolve `//> using file` directive" in {
    val additionalFile2 = os.temp(
      contents = """val predef2 = 10""",
      suffix = "additionalFile2"
    )
    val additionalFile1 = os.temp(
      contents = s"""//> using file $additionalFile2
                    |val predef1 = 20""".stripMargin,
      suffix = "additionalFile1"
    )
    val predefFile = os.temp(
      contents = s"""//> using file $additionalFile1
                    |val predef0 = 0""".stripMargin)

    UsingDirectives.findImportedFilesRecursively(predefFile.toNIO).sorted shouldBe
      Seq(additionalFile1, additionalFile2).map(_.toNIO).sorted
  }

  "recursively resolve `//> using file` directive - and handle recursive loops" in {
    val additionalFile2 = os.temp(suffix = "additionalFile2")
    val additionalFile1 = os.temp(suffix = "additionalFile1")
    val predefFile = os.temp(
      contents = s"""//> using file $additionalFile1
                    |val predef0 = 0""".stripMargin)

    os.write.over(additionalFile1,
      s"""//> using file $additionalFile2
         |val predef1 = 10""".stripMargin)
    os.write.over(additionalFile2,
      s"""//> using file $additionalFile1
         |val predef2 = 20""".stripMargin)

    UsingDirectives.findImportedFilesRecursively(predefFile.toNIO).sorted shouldBe
      Seq(additionalFile1, additionalFile2).map(_.toNIO).sorted
      // most importantly, this should not loop endlessly due to the recursive imports
  }

  "find declared dependencies" in {
    val source =
      """
        |//> using dep com.example:some-dependency:1.1
        |//> using dep com.example::scala-dependency:1.2
        |// //> using dep commented:out:1.3
        |""".stripMargin

    val results = UsingDirectives.findDeclaredDependencies(source.linesIterator)
    results should contain("com.example:some-dependency:1.1")
    results should contain("com.example::scala-dependency:1.2")
    results should not contain "commented:out:1.3"
  }

  "find declared resolvers" in {
    val source =
      """
        |//> using resolver https://repository.apache.org/content/groups/public
        |//> using resolver https://shiftleft.jfrog.io/shiftleft/libs-release-local
        |// //> using resolver https://commented.out/repo
        |""".stripMargin

    val results = UsingDirectives.findResolvers(source.linesIterator)
    results should contain("https://repository.apache.org/content/groups/public")
    results should contain("https://shiftleft.jfrog.io/shiftleft/libs-release-local")
    results should not contain "https://commented.out/repo"
  }

  if (scala.util.Properties.isWin) {
    info("paths work differently on windows - ignoring some tests")
  } else {
    "find declared classpath entries" in {
      val scriptFile = os.temp(
        """
          |//> using classpath /path/to/cp1
          |//> using classpath path/to/cp2
          |//> using classpath ../path/to/cp3
          |// //> using classpath cp4
          |""".stripMargin
      ).toNIO

      val scriptParentDir = scriptFile.getParent

      val results = UsingDirectives.findClasspathEntries(Seq(scriptFile))
      results should contain(Path.of("/path/to/cp1"))
      results should contain(scriptParentDir.resolve("path/to/cp2"))
      results should contain(scriptParentDir.resolve("../path/to/cp3"))
      results should not contain Path.of("cp3")
      results.size shouldBe 3 // just to triple check
    }
  }

}
