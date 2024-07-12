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
                 onExitCode: Option[String] = None,
                 greeting: Option[String],
                 prompt: String,
                 maxHeight: Option[Int] = None,
                 classLoader: Option[ClassLoader] = None)(using Colors)
  extends ReplDriverBase(compilerArgs, out, maxHeight, classLoader) {

  /** Run REPL with `state` until `:quit` command found
    * Main difference to the 'original': different greeting, trap Ctrl-c
   */
  override def runUntilQuit(using initialState: State = initialState)(): State = {
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
          onExitCode.foreach(code => run(code)(using state))
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
      val comps = completionsWithSignatures(line.cursor, line.line, state)
      /**
       * for Int.<tab>: lineWord="", filteredCompletions: Nil, all completions:
       * Completion(##,=> Int,List(method ##))
       * Completion(MinValue,(-2147483648 : Int),List(val MinValue))
       * Completion(#|^,(using x$2: replpp.Colors)(commandAndArguments: String*): Unit,List(method #|^))
       * Completion(==,(x$0: Any): Boolean,List(method ==))
       * Completion(hashCode,(): Int,List(method hashCode))
       * Completion(ne,(x$0: Object): Boolean,List(method ne))
       * Completion(#>>,(using x$2: replpp.Colors)(outFileName: String): Unit,List(method #>>))
       * Completion(#>>,(using x$2: replpp.Colors)(outFile: java.nio.file.Path): Unit,List(method #>>))
       * Completion(formatted,(fmtstr: String): String,List(method formatted))
       * Completion(box,(x: Int): Integer,List(method box))
       * Completion(int2long,(x: Int): Long,List(method int2long))
       * Completion(nn,=> Int.type & Int.type,List(method nn))
       * Completion(notifyAll,(): Unit,List(method notifyAll))
       * Completion(unbox,(x: Object): Int,List(method unbox))
       * Completion(synchronized,[X0](x$0: X0): X0,List(method synchronized))
       * Completion(#|,(using x$2: replpp.Colors)(commandAndArguments: String*): String,List(method #|))
       * Completion(toString,(): String,List(method toString))
       * Completion(!=,(x$0: Any): Boolean,List(method !=))
       * Completion(equals,(x$0: Any): Boolean,List(method equals))
       * Completion(eq,(x$0: Object): Boolean,List(method eq))
       * Completion(isInstanceOf,[X0]: Boolean,List(method isInstanceOf))
       * Completion(int2double,(x: Int): Double,List(method int2double))
       * Completion(ensuring,(cond: Boolean): A,List(method ensuring))
       * Completion(ensuring,(cond: A => Boolean): A,List(method ensuring))
       * Completion(ensuring,(cond: A => Boolean, msg: => Any): A,List(method ensuring))
       * Completion(ensuring,(cond: Boolean, msg: => Any): A,List(method ensuring))
       * Completion(wait,(x$0: Long, x$1: Int): Unit,List(method wait))
       * Completion(wait,(x$0: Long): Unit,List(method wait))
       * Completion(wait,(): Unit,List(method wait))
       * Completion(MaxValue,(2147483647 : Int),List(val MaxValue))
       * Completion(notify,(): Unit,List(method notify))
       * Completion(asInstanceOf,[X0]: X0,List(method asInstanceOf))
       * Completion(->,[B](y: B): (A, B),List(method ->))
       * Completion(#>,(using x$2: replpp.Colors)(outFileName: String): Unit,List(method #>))
       * Completion(#>,(using x$2: replpp.Colors)(outFile: java.nio.file.Path): Unit,List(method #>))
       * Completion(→,[B](y: B): (A, B),List(method →))
       * Completion(int2float,(x: Int): Float,List(method int2float))
       * Completion(getClass,[X0 >: Int.type](): Class[? <: X0],List(method getClass))
       */
      candidates.addAll(comps.map(_.label).distinct.map(makeCandidate).asJava)
      val lineWord = line.word()
      // this is only if there's an exact match
      comps.filter(c => c.label == lineWord && c.symbols.nonEmpty) match {
        case Nil =>
        case exactMatches =>
          val terminal = lineReader.nn.getTerminal
          lineReader.callWidget(LineReader.CLEAR)
          terminal.writer.println()
          for {
            exactMatch <- exactMatches
            sym <- exactMatch.symbols
          } terminal.writer.println(SyntaxHighlighting.highlight(sym.showUser))
          lineReader.callWidget(LineReader.REDRAW_LINE)
          lineReader.callWidget(LineReader.REDISPLAY)
          terminal.flush()
      }
    }

    terminal.readLine(completer).linesIterator
  }

  private def stripBackTicks(label: String) =
    if label.startsWith("`") && label.endsWith("`") then
      label.drop(1).dropRight(1)
    else
      label

}
