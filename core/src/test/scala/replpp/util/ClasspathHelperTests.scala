package replpp.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import replpp.Config

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
      val config = Config(classpathConfig = Config.ForClasspath(dependencies = Seq(
          "org.scala-lang:scala-library:2.13.10",
          "org.scala-lang::scala3-library:3.3.0",
        )))
      val deps = ClasspathHelper.dependencyArtifacts(config, scriptLines = Seq.empty)
      deps.size shouldBe 2

      assert(deps.find(_.endsWith("scala3-library_3-3.3.0.jar")).isDefined)
      assert(deps.find(_.endsWith("scala-library-2.13.10.jar")).isDefined)
    }

    "declared in scriptFile" in {
      val deps = ClasspathHelper.dependencyArtifacts(Config(), scriptLines = Seq("//> using dep com.michaelpollmeier::colordiff:0.36"))
      deps.size shouldBe 4

      assert(deps.find(_.endsWith("colordiff_3-0.36.jar")).isDefined)
      assert(deps.find(_.endsWith("scala3-library_3-3.3.0.jar")).isDefined)
      assert(deps.find(_.endsWith("diffutils-1.3.0.jar")).isDefined)
      assert(deps.find(_.endsWith("scala-library-2.13.10.jar")).isDefined)
    }
  }

}
