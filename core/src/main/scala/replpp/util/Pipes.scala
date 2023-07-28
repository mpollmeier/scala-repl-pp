package replpp.util

import java.nio.file.{Files, Path, Paths}
import scala.sys.process.Process
import scala.util.{Try, Using}

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

    // TODO document
    /** Pipe output into a different command.
     */
    def #|(command: String): Unit = {
      val tempFile = Files.createTempFile("replpp-pipes", "txt")
      val attempt = try {
        #>(tempFile)
        val p = Process(Seq(command, tempFile.toString))
        p.run()
        ()
      } finally {
        Files.deleteIfExists(tempFile)
      }
    }
  }

  def main(args: Array[String]): Unit = {
    import scala.sys.process._
    ???


  }


}
