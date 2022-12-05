package replpp

import dotty.tools.MainGenericCompiler.classpathSeparator
import dotty.tools.dotc.Run
import dotty.tools.dotc.core.Comments.{ContextDoc, ContextDocstrings}
import dotty.tools.dotc.core.Contexts.{Context, ContextBase, ContextState, FreshContext, ctx}
import dotty.tools.dotc.ast.{Positioned, tpd, untpd}
import dotty.tools.dotc.classpath.{AggregateClassPath, ClassPathFactory}
import dotty.tools.dotc.config.{Feature, JavaPlatform, Platform}
import dotty.tools.dotc.core.{Contexts, MacroClassLoader, Mode, TyperState}
import dotty.tools.io.{AbstractFile, ClassPath, ClassRepresentation}
import dotty.tools.repl.{AbstractFileClassLoader, CollectTopLevelImports, JLineTerminal, Newline, ParseResult, Parsed, Quit, State}

import java.io.PrintStream
import org.jline.reader.*

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

    /** Blockingly read a line, getting back a parse result */
    def readLine(state: State): ParseResult = {
      val completer: Completer = { (_, line, candidates) =>
        val comps = completions(line.cursor, line.line, state)
        candidates.addAll(comps.asJava)
      }
      given Context = state.context
      try {
        val lines = terminal.readLine(completer).split(lineSeparator)
        val linesFinal = substituteFileImportsWithContents(lines)

        ParseResult(linesFinal.mkString(lineSeparator))(using state)
      } catch {
        case _: EndOfFileException => // Ctrl+D
          onExitCode.foreach(code => run(code)(using state))
          Quit
        case _: UserInterruptException => // Ctrl+C
          Newline
      }
    }

    @tailrec def loop(using state: State)(): State = {
      val res = readLine(state)
      if (res == Quit) state
      else loop(using interpret(res))()
    }

    try runBody { loop(using initialState)() }
    finally terminal.close()
  }
  
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

  /**
    * for each `//> using file abc.sc` in the given `lines`, substitute it with the referenced file
    * this works recursively, i.e. if the referenced file references further files, those will be read as well
    */
  private def substituteFileImportsWithContents(lines: IterableOnce[String]): Seq[String] = {
    val trimmed = inputLine.trim
    if (trimmed.startsWith(UsingDirectives.LibDirective)) {
      out.println(s"warning: `using lib` directive does not work as input in interactive REPL - please pass it via predef code or `--dependency` list instead")
      Nil
    } else if (trimmed.startsWith(UsingDirectives.FileDirective)) {
      val files = UsingDirectives.findImportedFilesRecursively(inputLine.split(lineSeparator))
      files.toSeq.flatMap { file =>
        val ret = os.read.lines(file)
        out.println(s"read $file (${ret.size} lines)")
        ret
      }
    } else {
      Nil
    }
  }


}
