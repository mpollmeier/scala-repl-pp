package replpp

import replpp.shaded.os.{ProcessInput, ProcessOutput, SubProcess}

import java.io.{BufferedReader, ByteArrayOutputStream, FileWriter, InputStreamReader, PipedInputStream, PipedOutputStream, PrintStream}
import java.lang.ProcessBuilder.Redirect
import java.lang.System.lineSeparator
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import replpp.shaded.os
import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.sys.process.Process
import scala.util.{Failure, Success, Try, Using}

/**
  * Operators to redirect output to files or pipe them into external commands / processes,
  * inspired by unix shell redirection and pipe operators: `>`, `>>` and `|`.
  * Naming convention: similar to scala.sys.process we prefix all operators with `#`
  * to avoid naming clashes with more basic operators like `>` for greater-than-comparisons.
  * */
object Operators {

  /** output from an external command, e.g. when using `#|` */
  case class ProcessResults(stdout: String, stderr: String)

  extension (value: String) {

    /** Redirect output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFile: Path): Unit =
      writeToFile(outFile, append = false)

    /** Redirect output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFileName: String): Unit =
      #>(Paths.get(outFileName))

    /** Redirect output into file, appending to that file - similar to `>>` redirection in unix. */
    def #>>(outFile: Path): Unit =
      writeToFile(outFile, append = true)

    /** Redirect output into file, appending to that file - similar to `>>` redirection in unix. */
    def #>>(outFileName: String): Unit =
      #>>(Paths.get(outFileName))

    /**
     * Pipe output into an external process, i.e. pass the value into the command's InputStream.
     * Returns a concatenation of the stdout and stderr of the external command.
     * Executing an external command may fail, and this will throw an exception in that case.
     * see `#|^` for a variant that inherits IO (e.g. for `less`)
     */
    def #|(command: String): String = {
      val ProcessResults(stdout, stderr) = pipeToCommand(value, command, inheritIO = false).get
      Seq(stdout, stderr).filter(_.nonEmpty).mkString(lineSeparator)
    }

    /**
     * Pipe output into an external process, i.e. pass the value into the command's InputStream.
     * Executing an external command may fail, and this will throw an exception in that case.
     * This is a variant of `#|` which inherits IO (e.g. for `less`) - therefor it doesn't capture stdout/stderr. */
    def #|^(command: String): Unit =
      pipeToCommand(value, command, inheritIO = true).get

    private def writeToFile(outFile: Path, append: Boolean): Unit = {
      Using.resource(new FileWriter(outFile.toFile, append)) { fw =>
        fw.write(value)
        /* The convention on UNIX-like systems is that text files are supposed to end with a new-line.
         * This is mostly a side-effect of the fact that UNIX tools must end their output with a newline if they
         * don't want to mess up the prompt of the shell. And then piping the output of these tools to a file
         * means these files always end with a newline. And so appending on the shell also just needs to do
         * open(.., O_APPEND) and the appended output will automatically start in a new line.
         */
        fw.write(lineSeparator)
      }
    }
  }

  extension (iter: IterableOnce[_]) {
    private def valueAsString: String = iter.iterator.mkString(lineSeparator)

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

    /**
     * Pipe output into an external process, i.e. pass the value into the command's InputStream.
     * Returns a concatenation of the stdout and stderr of the external command.
     * Executing an external command may fail, and this will throw an exception in that case.
     * see `#|^` for a variant that inherits IO (e.g. for `less`)
     */
    def #|(command: String): String = {
      val ProcessResults(stdout, stderr) = pipeToCommand(valueAsString, command, inheritIO = false).get
      Seq(stdout, stderr).filter(_.nonEmpty).mkString(lineSeparator)
    }

    /**
     * Pipe output into an external process, i.e. pass the value into the command's InputStream.
     * Executing an external command may fail, and this will throw an exception in that case.
     * This is a variant of `#|` which inherits IO (e.g. for `less`) - therefor it doesn't capture stdout/stderr. */
    def #|^(command: String): Unit =
      pipeToCommand(valueAsString, command, inheritIO = true).get

  }

  extension (iter: java.lang.Iterable[_]) {

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

    /**
     * Pipe output into an external process, i.e. pass the value into the command's InputStream.
     * Returns a concatenation of the stdout and stderr of the external command.
     * Executing an external command may fail, and this will throw an exception in that case.
     * see `#|^` for a variant that inherits IO (e.g. for `less`)
     */
    def #|(command: String): String =
      iter.asScala #| command

    /**
     * Pipe output into an external process, i.e. pass the value into the command's InputStream.
     * Executing an external command may fail, and this will throw an exception in that case.
     * This is a variant of `#|` which inherits IO (e.g. for `less`) - therefor it doesn't capture stdout/stderr. */
    def #|^(command: String): Unit =
      iter.asScala #|^ command
  }

  /**
   * Pipe output into an external process, i.e. pass the value into the command's InputStream.
   * Executing an external command may fail, hence returning a `Try`.
   *
   * @param inheritIO : set to true for commands like `less` that are supposed to capture the entire IO
   */
  def pipeToCommand(value: String, command: String, inheritIO: Boolean): Try[ProcessResults] = {
    val stdout = new StringBuilder
    val stderr = new StringBuilder

    Try {
      os.proc(Seq(command)).call(
        stdin = pipeInput(value),
        stdout =
          if (inheritIO) os.Inherit
          else lineReader(stdout),
        stderr =
          if (inheritIO) os.Inherit
          else lineReader(stderr),
      )

      ProcessResults(stdout.result(), stderr.result())
    }
  }

  private def pipeInput(value: String) = new ProcessInput {
    def redirectFrom: Redirect = ProcessBuilder.Redirect.PIPE

    def processInput(stdin: => SubProcess.InputStream): Option[Runnable] = {
      Some { () =>
        val bytes = value.getBytes(StandardCharsets.UTF_8)
        val chunkSize = 8192
        var remaining = bytes.length
        var pos = 0
        while (remaining > 0) {
          val currentWindow = math.min(remaining, chunkSize)
          stdin.buffered.write(value, pos, currentWindow)
          pos += currentWindow
          remaining -= currentWindow
        }

        stdin.flush()
        stdin.close()
      }
    }
  }

  private def lineReader(stringBuilder: StringBuilder): os.ProcessOutput = {
    os.ProcessOutput.Readlines { s =>
      if (stringBuilder.nonEmpty) {
        stringBuilder.addAll(lineSeparator)
      }
      stringBuilder.addAll(s)
    }
  }

}
