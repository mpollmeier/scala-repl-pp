package replpp

import replpp.shaded.os.{ProcessInput, ProcessOutput, SubProcess}

import java.io.{BufferedReader, ByteArrayOutputStream, FileWriter, InputStreamReader, PipedInputStream, PipedOutputStream, PrintStream}
import java.lang.ProcessBuilder.Redirect
import java.lang.System.lineSeparator
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
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
     * It returns a concatenation of the stdout and stderr of the external command.
     * Executing an external command may fail, and this will throw an exception in that case.
     * If you See so the safe variant of this is `##|` which returns a `Try[ProcessResults]`.
     */
    def #|(command: String): String = {
      val ProcessResults(stdout, stderr) = ##|(command).get
      Seq(stdout, stderr).filter(_.nonEmpty).mkString(lineSeparator)
    }

    /**
     * Pipe output into an external process, i.e. pass the value into the command's InputStream.
     * Executing an external command may fail, hence returning a `Try`.
     */
    def ##|(command: String): Try[ProcessResults] = {
      import replpp.shaded.os
      var stdout = ""
      var stderr = ""

      // TODO experiment: capture System.out
      // TODO: fix threading issue: change `out` to my custom one at the start, and register handlers when needed; worst case that way: when two threads do this at the same time, we capute some stdout on both sides
      println("DD0: modifying stdout")
      val baos = new ByteArrayOutputStream
      val ps = new PrintStream(baos)
      val oldSystemOut = System.out
      System.setOut(ps)

      val stdoutImpl: os.ProcessOutput =
         new ProcessOutput {
           // works for `less` but not for `cat`: os.Inherit
           def redirectTo = ProcessBuilder.Redirect.INHERIT
           // works for `cat`, but not for `less`: Readlines(f: String => Unit)
           //           def redirectTo = ProcessBuilder.Redirect.PIPE

           //           def processOutput(stdin: => SubProcess.OutputStream) = None
           def processOutput(out: => SubProcess.OutputStream) = Some {
             () => {
               val buffered = new BufferedReader(new InputStreamReader(out))
               while ( {
                 val lineOpt =
                   try {
                     buffered.readLine() match {
                       case null =>
                         println("XX0")
                         None
                       case line =>
                         println("XX1")
                         Some(line)
                     }
                   } catch {
                     case e: Throwable => None
                   }
                 lineOpt match {
                   case None =>
                     println("XX2")
                     false
                   case Some(s) =>
                     println("XX3")
                     stdout = s
                     true
                 }
               }) ()
             }
           }
         }

      Try {
        os.proc(Seq(command)).call(
          stdin = pipeInput,
          stdout = stdoutImpl,
          stderr = os.ProcessOutput.Readlines { commandStderr =>
            stderr = commandStderr
          }
        )

        // revert to old system.out
        System.out.flush()
        System.setOut(oldSystemOut)
        val captured = baos.toString(StandardCharsets.UTF_8)
        baos.close()
        ps.close()
        println(s"DD9: captured = `$captured`")
        ProcessResults(stdout, stderr)
      }
    }

    private def pipeInput = new ProcessInput {
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

    private def writeToFile(outFile: Path, append: Boolean): Unit = {
      Using.resource(new FileWriter(outFile.toFile, true)) { fw =>
        fw.write(value)
        fw.write(lineSeparator)
      }
    }
  }

  extension (iter: IterableOnce[String]) {
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
     * It returns a concatenation of the stdout and stderr of the external command.
     * Executing an external command may fail, and this will throw an exception in that case.
     * If you See so the safe variant of this is `##|` which returns a `Try[ProcessResults]`.
     */
    def #|(command: String): String =
      valueAsString #| command

    /**
     * Pipe output into an external process, i.e. pass the value into the command's InputStream.
     * Executing an external command may fail, hence returning a `Try`.
     */
    def ##|(command: String): Try[ProcessResults] =
      valueAsString ##| command

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

    /**
     * Pipe output into an external process, i.e. pass the value into the command's InputStream.
     * It returns a concatenation of the stdout and stderr of the external command.
     * Executing an external command may fail, and this will throw an exception in that case.
     * If you See so the safe variant of this is `##|` which returns a `Try[ProcessResults]`.
     */
    def #|(command: String): String =
      iter.asScala #| command

    /**
     * Pipe output into an external process, i.e. pass the value into the command's InputStream.
     * Executing an external command may fail, hence returning a `Try`.
     */
    def ##|(command: String): Try[ProcessResults] =
      iter.asScala ##| command
  }

}
