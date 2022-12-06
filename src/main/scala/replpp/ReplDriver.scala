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
      val parseResult = readLine(terminal, state)
      if (parseResult == Quit) state
      else loop(using interpret(parseResult))()
//      else {
//        val state0 = interpret(parseResult)
//        if (usingFileDirectiveAndRemainder.isEmpty) loop(using state0)()
//        else {
//          val usingDirective = usingFileDirectiveAndRemainder.next()
//           TODO recursive...
//          ???
//        }
//      }
    }

    try runBody { loop(using initialState)() }
    finally terminal.close()
  }

  /** Blockingly read a line, getting back a parse result.
    * The input may be multi-line.
    * If the input contains a using file directive (e.g. `//> using file abc.sc`), then we interpret everything up
    * until the directive, then interpret the directive (i.e. import that file) and continue with the remainder of
    * our input. That way, we import the file in-place, while preserving line numbers for user feedback.  */
  private def readLine(terminal: JLineTerminal, state: State): ParseResult = {
    given Context = state.context
    val completer: Completer = { (_, line, candidates) =>
      val comps = completions(line.cursor, line.line, state)
      candidates.addAll(comps.asJava)
    }
    parseLines(terminal.readLine(completer).split(lineSeparator).iterator, state, os.pwd)
    // TODO reduce Seq[ParseResult] => ParseResult? or rather handle multiple parseResults..!
    ???
    // TODO: if there's a remainder iter, we may have encountered a file directive - interpret it and call ourselves recursively, updating the state
  }

  private def parseLines(lines: IterableOnce[String], state: State, currentFile: os.Path): Seq[ParseResult] = {
    val linesIter = lines.iterator
    val linesBeforeUsingFileDirectiveBuilder = Seq.newBuilder[String]
    val parseResults = Seq.newBuilder[ParseResult]
    while (linesIter.hasNext) {
      val line = linesIter.next()
      if (!line.trim.startsWith(UsingDirectives.FileDirective)) {
        // TODO extract
        val linesBeforeUsingFileDirective = linesBeforeUsingFileDirectiveBuilder.result()
        linesBeforeUsingFileDirectiveBuilder.clear()
        val state0 =
          if (linesBeforeUsingFileDirective.isEmpty) state
          else {
            // interpret everything until here, then interpret the given file (recursively call parseLine?) with content of file, then continue with the remainder of the linerIterator
            val parseResult0 = parseLine(linesBeforeUsingFileDirective.mkString(lineSeparator), state)
            interpret(parseResult0)(using state)
          }

        val pathStr = line.trim.drop(UsingDirectives.FileDirective.length)
        val file = resolveFile(currentFile, pathStr)
        println(s"> importing $file...")
        val linesFromFile = os.read.lines(file)
        parseResults.addAll(parseLines(linesFromFile, state0, file))
        // TODO handle remainder of call above, and remainder of current lineIter
        ???
      } else {
        linesBeforeUsingFileDirectiveBuilder.addOne(line)
      }
    }

    parseResults.result()
  }

  private def parseLine(line: String, state: State): ParseResult = {
    try {
      ParseResult(line)(using state)
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
