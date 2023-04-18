package replpp

import coursier.cache.{CacheDefaults, FileCache}
import coursier.credentials.Credentials
import coursier.parse.DependencyParser

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DependenciesTests extends AnyWordSpec with Matchers {
  val coursierCache = os.home / ".cache" / "coursier" / "v1"

  /** these require internet access... */
  "artifact resolution including transitive dependencies" should {
    "work for java artifacts" in {
      val jars = Dependencies.resolve(Seq("io.shiftleft:overflowdb-core:1.171")).get
      jars foreach println
      val overflowDbJar = coursierCache / os.RelPath("https/repo1.maven.org/maven2/io/shiftleft/overflowdb-core/1.171/overflowdb-core-1.171.jar")
      val mvstoreJar = coursierCache / os.RelPath("https/repo1.maven.org/maven2/com/h2database/h2-mvstore/1.4.200/h2-mvstore-1.4.200.jar")
      jars should contain(overflowDbJar.toIO)
      jars should contain(mvstoreJar.toIO)
    }

    "work for scala artifacts" in {
      val jars = Dependencies.resolve(Seq("com.lihaoyi::sourcecode:0.3.0")).get
      jars.size shouldBe 3
      val sourceCodeJar = coursierCache / os.RelPath("https/repo1.maven.org/maven2/com/lihaoyi/sourcecode_3/0.3.0/sourcecode_3-0.3.0.jar")
      val scalaLibJar = coursierCache / os.RelPath("https/repo1.maven.org/maven2/org/scala-lang/scala3-library_3/3.1.3/scala3-library_3-3.1.3.jar")
      jars should contain(sourceCodeJar.toIO)
      jars should contain(scalaLibJar.toIO)
    }
  }

  // TODO
  // bad cases: invalid coordinate, invalid repo

}

/** only to be run manually for now, sorry... */
object DependenciesTests {

  def main(args: Array[String]): Unit = {
    // verify that we can access an artifact that's only available on a separate, password-protected repository
    // note: relies on the local ~/config/coursier/credentials.properties
    // and the private jfrog artifactory in shiftleft.jfrog.io

    println(
      Dependencies.resolve(
        Seq("io.shiftleft::common:0.3.109"),
        Seq("https://shiftleft.jfrog.io/shiftleft/libs-release-local")
      ).get
    )
  }

}
