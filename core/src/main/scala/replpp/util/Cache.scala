package replpp.util

import replpp.home

import java.io.InputStream
import java.net.URI
import java.nio.file.{Files, Path}

/** A simple cache for `cacheKey` -> `Path`, where `Path` is a single file */
object Cache {
  lazy val Dir: Path = {
    val dir = home.resolve(".cache/scala-repl-pp")
    if (Files.notExists(dir)) Files.createDirectories(dir)
    dir
  }

  def getOrObtain(cacheKey: String, obtain: () => InputStream): Path = {
    val path = targetPath(cacheKey)
    this.synchronized {
      if (Files.exists(path)) {
        path
      } else {
        val inputStream = obtain()
        Files.copy(inputStream, path)
        inputStream.close()
        path
      }
    }
  }

  /** similar to `getOrObtain`, but specifically for files that need to be downloaded */
  def getOrDownload(cacheKey: String, downloadUrl: URI): Path = {
    getOrObtain(cacheKey, obtain = () => {
      downloadUrl.toURL.openStream()
    })
  }

  /**
   * @return true if cache entry did actually exist
   */
  def remove(cacheKey: String): Boolean = {
    val path = targetPath(cacheKey)
    val entryExisted = Files.exists(path)

    if (entryExisted)
      deleteRecursively(path)

    entryExisted
  }

  private def targetPath(cacheKey: String): Path =
    Dir.resolve(cacheKey)

}
