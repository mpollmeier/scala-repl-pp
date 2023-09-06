package replpp.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import replpp.Config
import replpp.util.ClasspathHelper.JarWithManifestInfo

import java.io.File.pathSeparator
import java.io.FileInputStream
import java.net.URI
import java.nio.file.{Files, Paths}

class ClasspathHelperTests extends AnyWordSpec with Matchers {

  "basic generation" in {
    ClasspathHelper.createAsSeq(Config()).size should be > 2
    // exact content depends on test run environment, since the current classpath is included as well
  }

  "must start and end with pathSeparator" in {
    // to circumvent a flakiness that caused much headaches
    val cp = ClasspathHelper.create(Config())
    cp should startWith(pathSeparator)
    cp should endWith(pathSeparator)
  }

  "resolves dependencies" when {
    "declared in config" in {
      val deps = ClasspathHelper.dependencyArtifacts(Config(dependencies = Seq(
        "org.scala-lang:scala-library:2.13.10",
        "org.scala-lang::scala3-library:3.3.0",
      )))
      deps.size shouldBe 2

      assert(deps.find(_.endsWith("scala3-library_3-3.3.0.jar")).isDefined)
      assert(deps.find(_.endsWith("scala-library-2.13.10.jar")).isDefined)
    }

    "declared in scriptFile" in {
      val deps = ClasspathHelper.dependencyArtifacts(Config(scriptFile = Some(os.temp(
        "//> using dep com.michaelpollmeier::colordiff:0.36"
      ).toNIO)))
      deps.size shouldBe 4

      assert(deps.find(_.endsWith("colordiff_3-0.36.jar")).isDefined)
      assert(deps.find(_.endsWith("scala3-library_3-3.3.0.jar")).isDefined)
      assert(deps.find(_.endsWith("diffutils-1.3.0.jar")).isDefined)
      assert(deps.find(_.endsWith("scala-library-2.13.10.jar")).isDefined)
    }
  }

  "picks highest version for each organization/name tuple (from the jar manifests)" in {
    val noManifest = Paths.get("no-manifest-info.jar")
    val name0 = Paths.get("only-name.jar")
    val org1Name1WithVersion1 = Paths.get("org1Name1WithVersion1.jar")
    val org1Name1WithVersion2 = Paths.get("org1Nme1WithVersion2.jar")
    val org2Name2WithVersion1 = Paths.get("org2Name2WithVersion1.jar")
    val org2Name2WithVersion2 = Paths.get("org2Name2WithVersion2.jar")
    val org2Name2WithVersion3 = Paths.get("org2Name2WithVersion3.jar")
    ClasspathHelper.onlyHighestVersionForEachJar(Seq(
      JarWithManifestInfo(noManifest, organisation = None, name = None, version = None),
      JarWithManifestInfo(name0, organisation = None, name = Some("name0"), version = None),

      JarWithManifestInfo(org1Name1WithVersion1, organisation = Some("org1"), name = Some("name1"), version = Some("1.0.0")),
      JarWithManifestInfo(org1Name1WithVersion2, organisation = Some("org1"), name = Some("name1"), version = Some("1.0.1")),

      JarWithManifestInfo(org2Name2WithVersion1, organisation = Some("org2"), name = Some("name2"), version = None),
      JarWithManifestInfo(org2Name2WithVersion2, organisation = Some("org2"), name = Some("name2"), version = Some("2.0.0")),
      JarWithManifestInfo(org2Name2WithVersion3, organisation = Some("org2"), name = Some("name2"), version = Some("2.0.0-RC3")),
    )).sorted shouldBe Seq(
      noManifest,
      name0,
      org1Name1WithVersion2,
      org2Name2WithVersion2
    ).sorted
  }
}
