package replpp

import replpp.shaded.os.{ProcessInput, ProcessOutput, SubProcess}

import java.io.FileWriter
import java.lang.ProcessBuilder.Redirect
import java.lang.System.lineSeparator
import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.sys.process.Process
import scala.util.{Try, Using}

/**
  * Operators to redirect output to files or pipe them into external commands / processes,
  * inspired by unix shell redirection and pipe operators: `>`, `>>` and `|`.
  * Naming convention: similar to scala.sys.process we prefix all operators with `#`
  * to avoid naming clashes with more basic operators like `>` for greater-than-comparisons.
  * */
object Operators {

  extension (value: String) {

    /** Redirect output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFile: Path): Unit =
      Files.writeString(outFile, value)

    /** Redirect output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFileName: String): Unit =
      #>(Paths.get(outFileName))

    /** Redirect output into file, appending to that file - similar to `>>` redirection in unix. */
    def #>>(outFile: Path): Unit = {
      Using.resource(new FileWriter(outFile.toFile, true)) { fw =>
        fw.write(lineSeparator)
        fw.write(value)
      }
    }

    /** Redirect output into file, appending to that file - similar to `>>` redirection in unix. */
    def #>>(outFileName: String): Unit =
      #>>(Paths.get(outFileName))

    /** Pipe output into a different command.
     * What actually happens: writes the string value to a temporary file and then passes that to the
     * given command. In other words, this does not stream the results.
     */
    def #|(command: String): Unit = {
      val tempFile = Files.createTempFile("replpp-piped", "txt")
      try {
        #>(tempFile)
        import replpp.shaded.os
        os.proc(Seq(command, tempFile.toString)).call(stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)
      } finally {
        Files.deleteIfExists(tempFile)
      }
    }
  }

  extension (iter: IterableOnce[String]) {
    private def valueAsString = iter.iterator.mkString(lineSeparator)

    /** Redirect output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFile: Path): Unit =
      valueAsString #> outFile

    /** Redirect output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFileName: String): Unit =
      valueAsString #> outFileName

    /** Redirect output into file, appending to that file - similar to `>>` redirection in unix. */
    def #>>(outFile: Path): Unit =
      valueAsString #>> outFile

    /** Redirect output into file, appending to that file - similar to `>>` redirection in unix. */
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

    /** Redirect output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFile: Path): Unit =
      iter.asScala #> outFile

    /** Redirect output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFileName: String): Unit =
      iter.asScala #> outFileName

    /** Redirect output into file, appending to that file - similar to `>>` redirection in unix. */
    def #>>(outFile: Path): Unit =
      iter.asScala #>> outFile

    /** Redirect output into file, appending to that file - similar to `>>` redirection in unix. */
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
