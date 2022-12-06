package replpp

import dotty.tools.MainGenericCompiler.classpathSeparator
import dotty.tools.dotc.Run
import dotty.tools.dotc.ast.{Positioned, tpd, untpd}
import dotty.tools.dotc.classpath.{AggregateClassPath, ClassPathFactory}
import dotty.tools.dotc.config.{Feature, JavaPlatform, Platform}
import dotty.tools.dotc.core.Comments.{ContextDoc, ContextDocstrings}
import dotty.tools.dotc.core.Contexts.{Context, ContextBase, ContextState, FreshContext, ctx}
import dotty.tools.dotc.core.{Contexts, MacroClassLoader, Mode, TyperState}
import dotty.tools.io.{AbstractFile, ClassPath, ClassRepresentation}
import dotty.tools.repl.*
import org.jline.reader.*

import java.io.PrintStream
import java.net.URL
import javax.naming.InitialContext
import scala.annotation.tailrec
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

class ReplDriver(args: Array[String],
                 out: PrintStream = scala.Console.out,
                 onExitCode: Option[String] = None,
                 greeting: Option[String],
                 prompt: String,
                 maxPrintElements: Int,
                 classLoader: Option[ClassLoader] = None) extends dotty.tools.repl.ReplDriver(args, out, classLoader) {

  lazy val lineSeparator = System.getProperty("line.separator")

  /** Run REPL with `state` until `:quit` command found
    * Main difference to the 'original': different greeting, trap Ctrl-c
   */
  override def runUntilQuit(using initialState: State = initialState)(): State = {
    val terminal = new JLineTerminal {
      override protected def promptStr = prompt
    }
    initializeRenderer()
    greeting.foreach(out.println)

    @tailrec
    def loop(using state: State)(): State = {
      // TODO use util.Try?
      val newStateMaybe: Option[State] = {
        try {
          val inputLines = readLine(terminal, state)
          Some(interpretInput(inputLines, state, os.pwd))
        } catch {
          case _: EndOfFileException => // Ctrl+D
            onExitCode.foreach(code => run(code)(using state))
            None
          case _: UserInterruptException => // Ctrl+C
            None
        }
      }

      newStateMaybe match {
        case Some(newState) => loop(using newState)()
        case None => state
      }
    }

    try runBody { loop(using initialState)() }
    finally terminal.close()
  }

  /** Blockingly read a line, getting back a parse result.
    * The input may be multi-line.
    * If the input contains a using file directive (e.g. `//> using file abc.sc`), then we interpret everything up
    * until the directive, then interpret the directive (i.e. import that file) and continue with the remainder of
    * our input. That way, we import the file in-place, while preserving line numbers for user feedback.  */
  private def readLine(terminal: JLineTerminal, state: State): IterableOnce[String] = {
    given Context = state.context
    val completer: Completer = { (_, line, candidates) =>
      val comps = completions(line.cursor, line.line, state)
      candidates.addAll(comps.asJava)
    }
    terminal.readLine(completer).split(lineSeparator).iterator
  }

  private def interpretInput(lines: IterableOnce[String], state: State, currentFile: os.Path): State = {
    val parsedLines = Seq.newBuilder[String]
    var resultingState = state

    for (line <- lines.iterator) {
      if (line.trim.startsWith(UsingDirectives.FileDirective)) {
        // TODO extract method for readability of surrounding condition
        val linesBeforeUsingFileDirective = parsedLines.result()
        parsedLines.clear()
        if (linesBeforeUsingFileDirective.nonEmpty)  {
          // interpret everything until here
          val parseResult = parseInput(linesBeforeUsingFileDirective, resultingState)
          resultingState = interpret(parseResult)(using resultingState)
        }

        // now read and interpret the given file
        val pathStr = line.trim.drop(UsingDirectives.FileDirective.length)
        val file = resolveFile(currentFile, pathStr)
        println(s"> importing $file")
        val linesFromFile = os.read.lines(file)
        resultingState = interpretInput(linesFromFile, resultingState, file)

        // finally, continue with the remainder of the current lines interator
      } else {
        parsedLines.addOne(line)
      }
    }

    val parseResult = parseInput(parsedLines.result(), resultingState)
    resultingState = interpret(parseResult)(using resultingState)
    resultingState
  }

  private def parseInput(lines: IterableOnce[String], state: State): ParseResult =
    parseInput(lines.iterator.mkString(lineSeparator), state)

  private def parseInput(input: String, state: State): ParseResult =
    ParseResult(input)(using state)

  /** configure rendering to use our pprinter for displaying results */
  private def initializeRenderer() = {
    rendering.myReplStringOf = {
      // We need to use the PPrinter class from the on the user classpath, and not the one available in the current
      // classloader, so we use reflection instead of simply calling `replpp.PPrinter:apply`.
      // This is analogous to what happens in dotty.tools.repl.Rendering.
      val pprinter = Class.forName("replpp.PPrinter", true, rendering.myClassLoader)
      val renderer = pprinter.getMethod("apply", classOf[Object])
      (value: Object) => renderer.invoke(null, value).asInstanceOf[String]
    }
  }

}
