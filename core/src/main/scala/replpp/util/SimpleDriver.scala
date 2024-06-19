package replpp.util

import dotty.tools.dotc.Driver
import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.reporting.{ConsoleReporter, Diagnostic, Reporter}
import dotty.tools.dotc.util.SourcePosition
import dotty.tools.io.{AbstractFile, Directory, PlainDirectory, VirtualDirectory}
import replpp.scripting.CompilerError

import java.nio.file.{Files, Path}
import scala.language.unsafeNulls
import scala.util.Try

/** Compiles input files to a virtual directory
 *
 * TODO: cache results
 * i.e. store hash of all inputs?
 * that functionality must exist somewhere already, e.g. zinc incremental compiler, or even in dotty itself?
 */
class SimpleDriver(lineNumberReportingAdjustment: Int = 0) extends Driver {
  
  def compileAndGetOutputDir[A](compilerArgs: Array[String], inputFiles: Seq[Path], verbose: Boolean): Try[VirtualDirectory] =
    compile(compilerArgs, inputFiles, verbose) { (ctx, outDir) => outDir }
    
  /** compiles given inputFiles and returns root directory that contains the class and tasty files */
  def compile[A](compilerArgs: Array[String], inputFiles: Seq[Path], verbose: Boolean)(fun: (Context, VirtualDirectory) => A): Try[A] = {
    if (verbose) {
      println(s"compiler arguments: ${compilerArgs.mkString(",")}")
      println(s"inputFiles: ${inputFiles.mkString(";")}")
    }

    val inputFiles0 = inputFiles.map(pathAsString).toArray
    val allArgs = compilerArgs ++ inputFiles0
    Try {
      val (toCompile, rootCtx) = setup(allArgs, initCtx.fresh)
        .getOrElse(throw CompilerError(s"error during setup with args=`${allArgs.mkString(" ")}`, details should have been reported already on stderr/stdout"))
      val outDir = new VirtualDirectory("(virtual)")
      given ctx0: Context = {
        val ctx = rootCtx.fresh.setSetting(rootCtx.settings.outputDir, outDir)
        if (lineNumberReportingAdjustment != 0) ctx.setReporter(createAdjustedReporter(rootCtx.reporter))

        if (verbose) {
          ctx.setSetting(rootCtx.settings.help, true)
            .setSetting(rootCtx.settings.XshowPhases, true)
            .setSetting(rootCtx.settings.Vhelp, true)
            .setSetting(rootCtx.settings.Vprofile, true)
            .setSetting(rootCtx.settings.explain, true)
        } else ctx
      }

      if (doCompile(newCompiler, toCompile).hasErrors) {
        val msgAddonMaybe = if (verbose) "" else " - try `--verbose` for more output"
        throw CompilerError(s"Errors encountered during compilation$msgAddonMaybe")
      } else {
        fun(ctx0, outDir)
      }
    }
  }

  // creates a new reporter based on the original reporter that copies Diagnostic and changes line numbers
  private def createAdjustedReporter(originalReporter: Reporter): Reporter = {
    new Reporter {
      override def doReport(dia: Diagnostic)(using Context): Unit = {
        val adjustedPos = new SourcePosition(source = dia.pos.source, span = dia.pos.span, outer = dia.pos.outer) {
          override def line: Int = super.line + lineNumberReportingAdjustment
        }
        originalReporter.doReport(new Diagnostic(dia.msg, adjustedPos, dia.level))
      }
    }
  }
}
