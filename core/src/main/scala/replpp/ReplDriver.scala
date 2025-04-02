package replpp

import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.repl.*
import dotty.tools.repl.Rendering.showUser
import org.jline.reader.*

import java.io.PrintStream
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

class ReplDriver(compilerArgs: Array[String],
                 out: PrintStream = scala.Console.out,
                 greeting: Option[String],
                 prompt: String,
                 maxHeight: Option[Int] = None,
                 classLoader: Option[ClassLoader] = None,
                 runAfter: Seq[String] = Nil,
                 verbose: Boolean = false)(using Colors)
  extends ReplDriverBase(compilerArgs, out, maxHeight, classLoader) {

  /** Run REPL with `state` until `:quit` command found
    * Main difference to the 'original': different greeting, trap Ctrl-c
   */
  override def runUntilQuit(using initialState: State)(): State = {
    val terminal = new replpp.JLineTerminal {
      override protected def promptStr = prompt
    }
    greeting.foreach(out.println)

    @tailrec
    def loop(using state: State)(): State = {
      Try {
        val inputLines = readLine(terminal, state)
        interpretInput(inputLines, state, pwd)
      } match {
        case Success(newState) =>
          loop(using newState)()
        case Failure(_: EndOfFileException) =>
          // Ctrl+D -> user wants to quit
          runAfter.foreach { code =>
            if (verbose) println(code)
            run(code)(using state)
          }
          state
        case Failure(_: UserInterruptException) =>
          // Ctrl+C -> swallow, do nothing
          loop(using state)()
        case Failure(exception) =>
          throw exception
      }
    }

    try runBody {
      loop(using initialState)()
    }
    finally terminal.close()
  }

  /** Blockingly read a line, getting back a parse result.
    * The input may be multi-line.
    * If the input contains a using file directive (e.g. `//> using file abc.sc`), then we interpret everything up
    * until the directive, then interpret the directive (i.e. import that file) and continue with the remainder of
    * our input. That way, we import the file in-place, while preserving line numbers for user feedback.  */
  private def readLine(terminal: replpp.JLineTerminal, state: State): IterableOnce[String] = {
    given Context = state.context
    val completer: Completer = { (lineReader, line, candidates) =>
      def makeCandidate(label: String) = {
        new Candidate(
          /* value    = */ label,
          /* displ    = */ stripBackTicks(label), // displayed value
          /* group    = */ null,  // can be used to group completions together
          /* descr    = */ null,  // TODO use for documentation?
          /* suffix   = */ null,
          /* key      = */ null,
          /* complete = */ false  // if true adds space when completing
        )
      }
      val comps = completions(line.cursor, line.line, state)
      candidates.addAll(comps.map(_.label).distinct.map(makeCandidate).asJava)
      val lineWord = line.word()
      comps.filter(c => c.label == lineWord && c.symbols.nonEmpty) match
        case Nil =>
        case exachMatches =>
          val terminal = lineReader.nn.getTerminal
          lineReader.callWidget(LineReader.CLEAR)
          terminal.writer.println()
          exachMatches.foreach: exact =>
            exact.symbols.foreach: sym =>
              terminal.writer.println(SyntaxHighlighting.highlight(sym.showUser))
          lineReader.callWidget(LineReader.REDRAW_LINE)
          lineReader.callWidget(LineReader.REDISPLAY)
          terminal.flush()
    }

    terminal.readLine(completer).linesIterator
  }

  private def stripBackTicks(label: String) =
    if label.startsWith("`") && label.endsWith("`") then
      label.drop(1).dropRight(1)
    else
      label

}
