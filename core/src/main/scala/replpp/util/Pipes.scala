package replpp.util

import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.sys.process.Process
import scala.util.{Try, Using}
import System.lineSeparator
import java.io.FileWriter

/**
 * Redirect output to files or external commands / processes - inspired by unix pipes.
 * Naming convention: similar to scala.sys.process we prefix all operators with `#`
 * to avoid naming clashes with more basic operators like `>` for comparisons.
 * */
object Pipes {

  extension (value: String) {

    /** Pipe output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFile: Path): Unit =
      Files.writeString(outFile, value)

    /** Pipe output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFileName: String): Unit =
      #>(Paths.get(outFileName))

    /** Pipe output into file, appending to that file - similar to `>>` redirection in unix. */
    def #>>(outFile: Path): Unit = {
      Using.resource(new FileWriter(outFile.toFile, true)) { fw =>
        fw.write(lineSeparator)
        fw.write(value)
      }
    }

    /** Pipe output into file, appending to that file - similar to `>>` redirection in unix. */
    def #>>(outFileName: String): Unit =
      #>>(Paths.get(outFileName))

    /** Pipe output into a different command.
     * What actually happens: writes the string value to a temporary file and then passes that to the
     * given command. In other words, this does not stream the results.
     */
    def #|(command: String): Unit = {
      val tempFile = Files.createTempFile("replpp-pipes", "txt")
      try {
        #>(tempFile)
        import replpp.shaded.os
        os.proc(Seq(command, tempFile.toString)) .call(stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)
      } finally {
        Files.deleteIfExists(tempFile)
      }
    }
  }

  extension (iter: IterableOnce[String]) {
    private def valueAsString = iter.iterator.mkString(lineSeparator)

    /** Pipe output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFile: Path): Unit =
      valueAsString #> outFile

    /** Pipe output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFileName: String): Unit =
      valueAsString #> outFileName

    /** Pipe output into file, appending to that file - similar to `>>` redirection in unix. */
    def #>>(outFile: Path): Unit =
      valueAsString #>> outFile

    /** Pipe output into file, appending to that file - similar to `>>` redirection in unix. */
    def #>>(outFileName: String): Unit =
      valueAsString #>> outFileName

    /** Pipe output into a different command.
     * What actually happens: writes the string value to a temporary file and then passes that to the
     * given command. In other words, this does not stream the results.
     */
    def #|(command: String): Unit =
      valueAsString #| command
  }

  extension (iter: java.lang.Iterable[String]) {

    /** Pipe output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFile: Path): Unit =
      iter.asScala #> outFile

    /** Pipe output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFileName: String): Unit =
      iter.asScala #> outFileName

    /** Pipe output into file, appending to that file - similar to `>>` redirection in unix. */
    def #>>(outFile: Path): Unit =
      iter.asScala #>> outFile

    /** Pipe output into file, appending to that file - similar to `>>` redirection in unix. */
    def #>>(outFileName: String): Unit =
      iter.asScala #>> outFileName

    /** Pipe output into a different command.
     * What actually happens: writes the string value to a temporary file and then passes that to the
     * given command. In other words, this does not stream the results.
     */
    def #|(command: String): Unit =
      iter.asScala #| command
  }

}
