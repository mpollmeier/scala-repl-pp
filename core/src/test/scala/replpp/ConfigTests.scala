package replpp

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.Paths

class ConfigTests extends AnyWordSpec with Matchers {
  val apacheRepo = "https://repository.apache.org/content/groups/public"
  val sonatypeRepo = "https://oss.sonatype.org/content/repositories/public"

  "asJavaArgs (inverse of Config.parse) for ScriptingDriver" in {
    val config = Config(
      predefFiles = List(Paths.get("/some/path/predefFile1"), Paths.get("/some/path/predefFile2")),
      nocolors = true,
      verbose = true,
      dependencies = Seq("com.michaelpollmeier:versionsort:1.0.7", "foo:bar:1.2.3"),
      resolvers = Seq(apacheRepo, sonatypeRepo),
      maxPrintCharacters = Some(10000),
      scriptFile = Some(Paths.get("/some/script.sc")),
      command = Some("someCommand"),
      params = Map("param1" -> "value1", "param2" -> "222", "someEquation" -> "40 + 2 = 42"),
    )

    val javaArgs = config.asJavaArgs
    javaArgs shouldBe Seq(
      "--predef", "/some/path/predefFile1",
      "--predef", "/some/path/predefFile2",
      "--nocolors",
      "--verbose",
      "--dep", "com.michaelpollmeier:versionsort:1.0.7",
      "--dep", "foo:bar:1.2.3",
      "--repo", apacheRepo,
      "--repo", sonatypeRepo,
      "--Vrepl-max-print-characters", "10000",
      "--script", "/some/script.sc",
      "--command", "someCommand",
      "--param", "param1=value1",
      "--param", "param2=222",
      "--param", "someEquation=40 + 2 = 42",
    )

    // round trip
    Config.parse(javaArgs.toArray) shouldBe config
  }

}
