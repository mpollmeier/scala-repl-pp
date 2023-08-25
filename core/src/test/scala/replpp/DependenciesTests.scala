package replpp

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.Path

class DependenciesTests extends AnyWordSpec with Matchers {
  val coursierCache = os.home / ".cache" / "coursier" / "v1"

  /** these require internet access... */
  "artifact resolution including transitive dependencies" should {
    "work for java artifacts" in {
      val jars = Dependencies.resolve(Seq("io.shiftleft:overflowdb-core:1.171")).get

      ensureContains("overflowdb-core-1.171.jar", jars)
      ensureContains("h2-mvstore-1.4.200.jar", jars)
    }

    "work for scala artifacts" in {
      val jars = Dependencies.resolve(Seq("com.lihaoyi::sourcecode:0.3.0")).get
      jars.size shouldBe 3
      ensureContains("sourcecode_3-0.3.0.jar", jars)
      ensureContains("scala3-library_3-3.1.3.jar", jars)
    }

    def ensureContains(fileName: String, jars: Seq[Path]): Unit = {
      assert(jars.exists(_.toString.endsWith(fileName)), s"expected $fileName, but it's not in the results: $jars")
    }
  }

  "return failure for invalid dependency coordinate" in {
    val parseResult = Dependencies.resolve(Seq("not-a-valid-maven-coordinate"))
    val errorMessage = parseResult.failed.get.getMessage
    errorMessage should include("fetch not-a-valid-maven-coordinate")
  }

  "return failure for invalid repository" in {
    val parseResult = Dependencies.resolve(
      Seq("com.lihaoyi::sourcecode:0.3.0"),
      Seq("not-a-valid-repository"),
    )
    val errorMessage = parseResult.failed.get.getMessage
    errorMessage should include("not-a-valid-repository")
  }
}

object DependenciesTests {

  def main(args: Array[String]): Unit = {
    /** only to be run manually, sorry...
      * verify that we can access an artifact that's only available on a separate, password-protected repository
      * note: relies on the local ~/.config/coursier/credentials.properties
      * and the private jfrog artifactory in shiftleft.jfrog.io
     */

    println(
      Dependencies.resolve(
        Seq("io.shiftleft::common:0.3.109"),
        Seq("https://shiftleft.jfrog.io/shiftleft/libs-release-local")
      ).get
    )
  }

}
