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
    // TODO
    ???
  }

  "find imported files recursively from given script" in {
    // TODO
    ???
  }

  "find declared dependencies" in {
    val source =
      """
        |//> using lib com.example:some-dependency:1.1
        |//> using lib com.example::scala-dependency:1.2
        |// //> using lib commented:out:1.3
        |""".stripMargin

    val results = UsingDirectives.findDeclaredDependencies(source)
    results should contain("com.example:some-dependency:1.1")
    results should contain("com.example::scala-dependency:1.2")
    results should not contain "commented:out:1.3"
  }

}
