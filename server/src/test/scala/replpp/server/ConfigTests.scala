package replpp.server

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import replpp.Colors

import java.nio.file.Paths

class ConfigTests extends AnyWordSpec with Matchers {
  val apacheRepo = "https://repository.apache.org/content/groups/public"

  "parse server and base config combined" in {
    val parsed = Config.parse(Array(
      "--server-host", "testHost",
      "--server-port", "42",
      "--server-auth-username", "test-user",
      "--server-auth-password", "test-pass",
      "--verbose",
      "--predef", "test-predef.sc",
    ))
    parsed.serverHost shouldBe "testHost"
    parsed.serverPort shouldBe 42
    parsed.serverAuthUsername shouldBe Some("test-user")
    parsed.serverAuthPassword shouldBe Some("test-pass")
    parsed.baseConfig.verbose shouldBe true
    parsed.baseConfig.predefFiles shouldBe Seq(Paths.get("test-predef.sc"))
  }

}
