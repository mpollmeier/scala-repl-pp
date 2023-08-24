package replpp.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import replpp.Colors

import java.io.FileInputStream
import java.nio.file.Paths

class CacheTests extends AnyWordSpec with Matchers {

  "caches a file by it's key" in {
    var obtainedFunctionInvocations = 0
    val cacheKey = "test-cacheKey"

    Cache.remove(cacheKey)
    Cache.getOrObtain(
      cacheKey,
      obtain = () => {
        obtainedFunctionInvocations += 1
        new FileInputStream(ProjectRoot.relativise("README.md").toFile)
      }
    )
    Cache.getOrObtain(
      cacheKey,
      obtain = () => {
        obtainedFunctionInvocations += 1
        new FileInputStream(ProjectRoot.relativise("README.md").toFile)
      }
    )

    obtainedFunctionInvocations shouldBe 1
    Cache.remove(cacheKey) shouldBe true
  }

}
