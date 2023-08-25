package replpp.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.FileInputStream
import java.net.URI
import java.nio.file.Files

class CacheTests extends AnyWordSpec with Matchers {

  "caches a file by it's key" in {
    var obtainedFunctionInvocations = 0
    val cacheKey = "test-cacheKey1"
    Cache.remove(cacheKey)
    
    Cache.getOrObtain(
      cacheKey,
      obtain = () => {
        obtainedFunctionInvocations += 1
        new FileInputStream(ProjectRoot.relativise("README.md").toFile)
      }
    )
    val cachedFile = Cache.getOrObtain(
      cacheKey,
      obtain = () => {
        obtainedFunctionInvocations += 1
        new FileInputStream(ProjectRoot.relativise("README.md").toFile)
      }
    )

    obtainedFunctionInvocations shouldBe 1
    Files.size(cachedFile) should be > 1024L
    Cache.remove(cacheKey) shouldBe true
  }

  "convenience function to download by URL" in {
    val cacheKey = "test-cacheKey2"
    Cache.remove(cacheKey)
    
    val cachedFile = Cache.getOrDownload(cacheKey, new URI("https://raw.githubusercontent.com/mpollmeier/scala-repl-pp/main/README.md"))

    Files.size(cachedFile) should be > 1024L
    Cache.remove(cacheKey) shouldBe true
  }

}
