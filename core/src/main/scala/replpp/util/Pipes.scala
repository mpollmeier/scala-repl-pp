package replpp.util

import java.nio.file.{Files, Path, Paths}
import scala.sys.process.Process
import scala.util.{Try, Using}
import System.lineSeparator

object Pipes {

  extension (string: String) {

    /** Pipe output into file, overriding that file.
     * Similar to unix' `>` redirection.
     * We're not using `>` because we don't want to overrode the regular comparison operator... */
    def #>(outFile: Path): Unit =
      Files.writeString(outFile, string)

    /** Pipe output into file, overriding that file.
     * Similar to unix' `>` redirection.
     * We're not using `>` because we don't want to overrode the regular comparison operator... */
    def #>(outFileName: String): Unit =
      #>(Paths.get(outFileName))

    // TODO implement |>>

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

    /** Pipe output into file, overriding that file.
     * Similar to unix' `>` redirection.
     * We're not using `>` because we don't want to overrode the regular comparison operator... */
    def #>(outFile: Path): Unit =
      Files.writeString(outFile, valueAsString)

    /** Pipe output into file, overriding that file.
     * Similar to unix' `>` redirection.
     * We're not using `>` because we don't want to overrode the regular comparison operator... */
    def #>(outFileName: String): Unit =
      #>(Paths.get(outFileName))

    // TODO implement |>>

    /** Pipe output into a different command.
     * What actually happens: writes the string value to a temporary file and then passes that to the
     * given command. In other words, this does not stream the results.
     */
    def #|(command: String): Unit = {
      val tempFile = Files.createTempFile("replpp-pipes", "txt")
      try {
        #>(tempFile)
        import replpp.shaded.os
        os.proc(Seq(command, tempFile.toString)).call(stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)
      } finally {
        Files.deleteIfExists(tempFile)
      }
    }
  }

}
