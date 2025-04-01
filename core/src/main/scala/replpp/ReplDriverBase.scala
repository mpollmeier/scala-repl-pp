package replpp

import dotty.tools.repl.*
import replpp.shaded.fansi

import java.io.PrintStream
import java.lang.System.lineSeparator
import java.nio.file.{Files, Path}

abstract class ReplDriverBase(compilerArgs: Array[String],
                              out: PrintStream,
                              maxHeight: Option[Int],
                              classLoader: Option[ClassLoader],
                              lineNumberReportingAdjustment: Int = 0)(using Colors)
  extends DottyReplDriver(compilerArgs, out, maxHeight, classLoader, lineNumberReportingAdjustment) {

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
