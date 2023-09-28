package replpp

import replpp.shaded.os
import replpp.shaded.os.{ProcessInput, ProcessOutput, SubProcess}

import java.io.FileWriter
import java.lang.ProcessBuilder.Redirect
import java.lang.System.lineSeparator
import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths}
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}

/**
 * Operators to redirect output to files or pipe them into external commands / processes,
 * inspired by unix shell redirection and pipe operators: `>`, `>>` and `|`.
 * Naming convention: similar to scala.sys.process we prefix all operators with `#`
 * to avoid naming clashes with more basic operators like `>` for greater-than-comparisons.
 *
 * They are declared as extension types for `Any` and pretty-print the results using our `PPrinter`.
 * List types (IterableOnce, java.lang.Iterable, Array, ...) are being unwrapped (only at the root level).
 * */
object Operators {

  /** output from an external command, e.g. when using `#|` */
  case class ProcessResults(stdout: String, stderr: String)

  extension (value: Any)(using Colors) {

    /** Redirect output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFile: Path): Unit =
      writeToFile(valueAsString, outFile, append = false)

    /** Redirect output into file, overriding that file - similar to `>` redirection in unix. */
    def #>(outFileName: String): Unit =
      #>(Paths.get(outFileName))

    /** Redirect output into file, appending to that file - similar to `>>` redirection in unix. */
    def #>>(outFile: Path): Unit =
      writeToFile(valueAsString, outFile, append = true)

    /** Redirect output into file, appending to that file - similar to `>>` redirection in unix. */
    def #>>(outFileName: String): Unit =
      #>>(Paths.get(outFileName))

    /**
     * Pipe output into an external process, i.e. pass the valueAsString into the command's InputStream.
     * Returns a concatenation of the stdout and stderr of the external command.
     * Executing an external command may fail, and this will throw an exception in that case.
     * see `#|^` for a variant that inherits IO (e.g. for `less`)
     */
    def #|(commandAndArguments: String*): String = {
      val ProcessResults(stdout, stderr) = pipeToCommand(valueAsString, commandAndArguments, inheritIO = false).get
      Seq(stdout, stderr).filter(_.nonEmpty).mkString(lineSeparator)
    }

    /**
     * Pipe output into an external process, i.e. pass the valueAsString into the command's InputStream.
     * Executing an external command may fail, and this will throw an exception in that case.
     * This is a variant of `#|` which inherits IO (e.g. for `less`) - therefor it doesn't capture stdout/stderr. */
    def #|^(commandAndArguments: String*): Unit =
      pipeToCommand(valueAsString, commandAndArguments, inheritIO = true).get


    /**
     * If `value` is a list-type: unwrap it (only at the root level).
     * Then, pretty-print the results using our `PPrinter`.
     * This is to ensure we get the same output as we would get on the REPL (apart from the list-unwrapping).
     */
    private def valueAsString: String = {
      val topLevelListTypeMaybe: Option[Iterator[_]] =
        value match {
          case iter: IterableOnce[_] => Some(iter.iterator)
          case iter: java.lang.Iterable[_] => Some(iter.iterator.asScala)
          case iter: java.util.Iterator[_] => Some(iter.asScala)
          case array: Array[_] => Some(array.iterator)
          case _ => None
        }

      topLevelListTypeMaybe match {
        case None =>
          render(value)
        case Some(iter) =>
          iter.map(render).mkString(lineSeparator)
      }
    }

    /**
     * Pretty-print the results using our `PPrinter`. Special handling for top level strings: render without quotes
     */
    private def render(obj: Any): String = {
      obj match {
        case string: String =>
          string
        case other =>
          PPrinter(
            other,
            nocolors = summon[Colors] == Colors.BlackWhite
          )
      }
    }
  }

  /**
   * Pipe output into an external process, i.e. pass the value into the command's InputStream.
   * Executing an external command may fail, hence returning a `Try`.
   *
   * @param inheritIO : set to true for commands like `less` that are supposed to capture the entire IO
   */
  def pipeToCommand(value: String, commandAndArguments: Seq[String], inheritIO: Boolean): Try[ProcessResults] = {
    val stdout = new StringBuilder
    val stderr = new StringBuilder

    Try {
      os.proc(commandAndArguments).call(
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

  private def writeToFile(value: String, outFile: Path, append: Boolean): Unit = {
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
