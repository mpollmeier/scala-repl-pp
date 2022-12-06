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
      val inputLines = readLine(terminal, state)
      val interpretResult = interpretInput(inputLines, state, os.pwd)
      if (interpretResult.shouldStop)
        interpretResult.state
      else
        loop(using interpretResult.state)()
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

  private class InterpretResult(var state: State, var shouldStop: Boolean = false)
  private def interpretInput(lines: IterableOnce[String], state: State, currentFile: os.Path): InterpretResult = {
    val parsedLines = Seq.newBuilder[String]
    var interpretResult = new InterpretResult(state)

    for (line <- lines.iterator) {
      if (line.trim.startsWith(UsingDirectives.FileDirective)) {
        // TODO extract method for readability of surrounding condition
        val linesBeforeUsingFileDirective = parsedLines.result()
        parsedLines.clear()
        if (linesBeforeUsingFileDirective.nonEmpty)  {
          // interpret everything until here, then interpret the given file, then continue with the remainder of the lines.iterator
          val parseResult = parseInput(linesBeforeUsingFileDirective, interpretResult.state)
          interpretResult.state = interpret(parseResult)(using interpretResult.state)
          if (interpretResult.state == Quit)
            interpretResult.shouldStop = true
        }

        val pathStr = line.trim.drop(UsingDirectives.FileDirective.length)
        val file = resolveFile(currentFile, pathStr)
        println(s"> importing $file...")
        val linesFromFile = os.read.lines(file)
        val interpretResult1 = interpretInput(linesFromFile, interpretResult, file)
        interpretResult.state = interpretResult1.state
        if (interpretResult1.shouldStop) interpretResult.shouldStop = true
      } else {
        parsedLines.addOne(line)
      }
    }

    val parseResult = parseInput(parsedLines.result(), state)
    interpretResult = interpret(parseResult)(using state)
    interpretResult
  }

  private def parseInput(lines: IterableOnce[String], state: State): ParseResult =
    parseInput(lines.iterator.mkString(lineSeparator), state)

  private def parseInput(input: String, state: State): ParseResult = {
    try {
      ParseResult(input)(using state)
    } catch {
      case _: EndOfFileException => // Ctrl+D
        onExitCode.foreach(code => run(code)(using state))
        Quit
      case _: UserInterruptException => // Ctrl+C
        Newline
    }
  }

//  /**
//    * for each `//> using file abc.sc` in the given `lines`, substitute it with the referenced file
//    * this works recursively, i.e. if the referenced file references further files, those will be read as well
//    */
//  private def substituteFileImportsWithContents(lines: IterableOnce[String]): Seq[String] = {
//    val trimmed = inputLine.trim
//    if (trimmed.startsWith(UsingDirectives.LibDirective)) {
//      out.println(s"warning: `using lib` directive does not work as input in interactive REPL - please pass it via predef code or `--dependency` list instead")
//      Nil
//    } else if (trimmed.startsWith(UsingDirectives.FileDirective)) {
//      val files = UsingDirectives.findImportedFilesRecursively(inputLine.split(lineSeparator))
//      files.toSeq.flatMap { file =>
//        val ret = os.read.lines(file)
//        out.println(s"read $file (${ret.size} lines)")
//        ret
//      }
//    } else {
//      Nil
//    }
//  }
//

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
