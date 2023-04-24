package replpp

import java.nio.file.Paths
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

  "find imported files recursively from given source" in {
    val script1 = os.temp("val foo = 42")
    val script2 = os.temp(
      s"""//> using file $script1
         |val bar = 42""".stripMargin)

    val rootPath = Paths.get(".")
    val source = s"//> using file $script2"

    val results = UsingDirectives.findImportedFilesRecursively(Seq(source), rootPath)
    results should contain(script1.toNIO)
    results should contain(script2.toNIO)
  }

  "find imported files recursively from given script" in {
    val script1 = os.temp("val foo = 42")
    val script2 = os.temp(
      s"""//> using file $script1
         |val bar = 42""".stripMargin)
    val script3 = os.temp(s"//> using file $script2")

    val results = UsingDirectives.findImportedFilesRecursively(script3.toNIO)
    results should contain(script1.toNIO)
    results should contain(script2.toNIO)
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

}
