package replpp

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ConfigTests extends AnyWordSpec with Matchers {
  val apacheRepo = "https://repository.apache.org/content/groups/public"
  val sonatypeRepo = "https://oss.sonatype.org/content/repositories/public"

  "asJavaArgs (inverse of Config.parse) for ScriptingDriver" in {
    val config = Config(
      predefCode = Some("val predefCode = 42"),
      predefFiles = List(os.Path("/some/path/predefFile1"), os.Path("/some/path/predefFile2")),
      nocolors = true,
      verbose = true,
      dependencies = Seq("com.michaelpollmeier:versionsort:1.0.7", "foo:bar:1.2.3"),
      resolvers = Seq(apacheRepo, sonatypeRepo),
      scriptFile = Some(os.Path("/some/script.sc")),
      command = Some("someCommand"),
      params = Map("param1" -> "value1", "param2" -> "222")
    )

    val javaArgs = config.asJavaArgs
    javaArgs shouldBe Seq(
      "--predefCode", "val predefCode = 42",
      "--predefFiles", "/some/path/predefFile1",
      "--predefFiles", "/some/path/predefFile2",
      "--nocolors",
      "--verbose",
      "--dependencies", "com.michaelpollmeier:versionsort:1.0.7",
      "--dependencies", "foo:bar:1.2.3",
      "--resolvers", apacheRepo,
      "--resolvers", sonatypeRepo,
      "--script", "/some/script.sc",
      "--command", "someCommand",
      "--params", "param1=value1,param2=222"
    )

    // round trip
    Config.parse(javaArgs.toArray) shouldBe config
  }

}
