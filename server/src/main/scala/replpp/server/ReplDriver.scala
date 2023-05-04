package replpp.server

import dotty.tools.MainGenericCompiler.classpathSeparator
import dotty.tools.dotc.Run
import dotty.tools.dotc.ast.{Positioned, tpd, untpd}
import dotty.tools.dotc.classpath.{AggregateClassPath, ClassPathFactory}
import dotty.tools.dotc.config.{Feature, JavaPlatform, Platform}
import dotty.tools.dotc.core.Comments.{ContextDoc, ContextDocstrings}
import dotty.tools.dotc.core.Contexts.{Context, ContextBase, ContextState, FreshContext, ctx, explore}
import dotty.tools.dotc.core.{Contexts, MacroClassLoader, Mode, TyperState}
import dotty.tools.io.{AbstractFile, ClassPath, ClassRepresentation}
import dotty.tools.repl.*
import org.jline.reader.*
import replpp.{UsingDirectives, pwd, resolveFile, util}

import java.io.PrintStream
import java.lang.System.lineSeparator
import java.net.URL
import java.nio.file.Path
import javax.naming.InitialContext
import scala.annotation.tailrec
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

class ReplDriver(args: Array[String],
                 out: PrintStream = scala.Console.out,
                 classLoader: Option[ClassLoader] = None) extends dotty.tools.repl.ReplDriver(args, out, classLoader) {

  rendering.myReplStringOf = (objectToRender: Object, maxElements: Int, maxCharacters: Int) => {
    val output = s"XXX0 rendering: $objectToRender"
    println("XXX1")
    output
  }

  def execute(inputLines: IterableOnce[String])(using state: State = initialState): State = {
     interpretInput(inputLines, state, pwd)
//    @tailrec
//    def loop(using state: State)(): State = {
//      Try {
//        interpretInput(inputLines, state, pwd)
//      } match {
//        case Success(newState) =>
//          loop(using newState)()
//        case Failure(_: EndOfFileException) =>
//          // Ctrl+D -> user wants to quit
//          onExitCode.foreach(code => run(code)(using state))
//          state
//        case Failure(_: UserInterruptException) =>
//          // Ctrl+C -> swallow, do nothing
//          loop(using state)()
//        case Failure(exception) =>
//          throw exception
//      }
//    }
//
//    try runBody { loop(using initialState)() }
  }

  private def interpretInput(lines: IterableOnce[String], state: State, currentFile: Path): State = {
    val parsedLines = Seq.newBuilder[String]
    var resultingState = state

    def handleImportFileDirective(line: String) = {
      val linesBeforeUsingFileDirective = parsedLines.result()
      parsedLines.clear()
      if (linesBeforeUsingFileDirective.nonEmpty) {
        // interpret everything until here
        val parseResult = parseInput(linesBeforeUsingFileDirective, resultingState)
        resultingState = interpret(parseResult)(using resultingState)
      }

      // now read and interpret the given file
      val pathStr = line.trim.drop(UsingDirectives.FileDirective.length)
      val path = resolveFile(currentFile, pathStr)
      val linesFromFile = util.linesFromFile(path)
      println(s"> importing $path (${linesFromFile.size} lines)")
      resultingState = interpretInput(linesFromFile, resultingState, path)
    }

    for (line <- lines.iterator) {
      if (line.trim.startsWith(UsingDirectives.FileDirective))
        handleImportFileDirective(line)
      else
        parsedLines.addOne(line)
    }

    val parseResult = parseInput(parsedLines.result(), resultingState)
    resultingState = interpret(parseResult)(using resultingState)
    resultingState
  }

  private def parseInput(lines: IterableOnce[String], state: State): ParseResult =
    parseInput(lines.iterator.mkString(lineSeparator), state)

  private def parseInput(input: String, state: State): ParseResult =
    ParseResult(input)(using state)

}
