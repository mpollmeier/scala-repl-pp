package replpp

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
import replpp.shaded.fansi

import java.io.PrintStream
import java.lang.System.lineSeparator
import java.net.URL
import java.nio.file.{Files, Path}
import javax.naming.InitialContext
import scala.annotation.tailrec
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

abstract class ReplDriverBase(args: Array[String],
                              out: PrintStream,
                              maxHeight: Option[Int],
                              classLoader: Option[ClassLoader])(using Colors)
  extends DottyReplDriver(args, out, maxHeight, classLoader) {

  protected def interpretInput(lines: IterableOnce[String], state: State, currentFile: Path): State = {
    val parsedLines = Seq.newBuilder[String]
    var currentState = state

    def handleImportFileDirective(line: String) = {
      val linesBeforeUsingFileDirective = parsedLines.result()
      parsedLines.clear()
      if (linesBeforeUsingFileDirective.nonEmpty) {
        // interpret everything until here
        val parseResult = parseInput(linesBeforeUsingFileDirective, currentState)
        currentState = interpret(parseResult)(using currentState)
      }

      // now read and interpret the given file
      val pathStr = line.trim.drop(UsingDirectives.FileDirective.length)
      val path = resolveFile(currentFile, pathStr)
      if (Files.exists(path)) {
        val linesFromFile = util.linesFromFile(path)
        println(s"> importing $path (${linesFromFile.size} lines)")
        currentState = interpretInput(linesFromFile, currentState, path)
      } else {
        System.err.println(util.colorise(s"Warning: given file `$path` does not exist.", fansi.Color.Red))
      }
    }

    for (line <- lines.iterator) {
      if (line.trim.startsWith(UsingDirectives.FileDirective))
        handleImportFileDirective(line)
      else
        parsedLines.addOne(line)
    }

    val parseResult = parseInput(parsedLines.result(), currentState)
    interpret(parseResult)(using currentState)
  }

  private def parseInput(lines: IterableOnce[String], state: State): ParseResult =
    parseInput(lines.iterator.mkString(lineSeparator), state)

  private def parseInput(input: String, state: State): ParseResult =
    ParseResult(input)(using state)

}
