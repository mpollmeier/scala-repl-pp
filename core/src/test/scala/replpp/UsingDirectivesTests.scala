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

}
