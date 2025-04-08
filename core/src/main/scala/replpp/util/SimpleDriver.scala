package replpp.util

import dotty.tools.dotc.Driver
import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.reporting.{Diagnostic, Reporter}
import dotty.tools.dotc.util.SourcePosition
import dotty.tools.io.{Directory, PlainDirectory}
import replpp.scripting.CompilerError

import java.nio.file.{Files, Path}
import scala.language.unsafeNulls
import scala.util.Try
import scala.util.control.NoStackTrace

/** Compiles input files to a temporary directory
 *
 * TODO: use a VirtualDirectory for the output - I didn't manage to find a good way to pass those to the
 * Context of the DottyReplDriver yet...
 * val virtualDirectory = new VirtualDirectory("(virtual)")
 * val cp = ClassPathFactory.newClassPath(virtualDirectory)
 *
 * TODO: cache results
 * i.e. store hash of all inputs?
 * that functionality must exist somewhere already, e.g. zinc incremental compiler, or even in dotty itself?
 */
class SimpleDriver(linesBeforeRunBeforeCode: Int = 0, linesBeforeScript: Int = 0) extends Driver {
  
  def compileAndGetOutputDir[A](compilerArgs: Array[String], inputFiles: Seq[Path], verbose: Boolean): Try[Path] =
    compile(compilerArgs, inputFiles, verbose) { (ctx, outDir) => outDir }
    
  /** compiles given inputFiles and returns root directory that contains the class and tasty files */
  def compile[A](compilerArgs: Array[String], inputFiles: Seq[Path], verbose: Boolean)(fun: (Context, Path) => A): Try[A] = {
    if (verbose) {
      println(s"compiler arguments: ${compilerArgs.mkString(",")}")
      println(s"inputFiles: ${inputFiles.mkString(";")}")
    }

    val inputFiles0 = inputFiles.map(pathAsString).toArray
    val allArgs = compilerArgs ++ inputFiles0
    Try {
      val (toCompile, rootCtx) = setup(allArgs, initCtx.fresh)
        .getOrElse(throw CompilerError(s"error during setup with args=`${allArgs.mkString(" ")}`, details should have been reported already on stderr/stdout"))

      val outDir = Files.createTempDirectory("scala-repl-pp")

      given ctx0: Context = {
        val ctx = rootCtx.fresh

        if (linesBeforeRunBeforeCode != 0 || linesBeforeScript != 0) {
          ctx.setReporter(createReporter(linesBeforeRunBeforeCode, linesBeforeScript, rootCtx.reporter))
        }

        ctx.setSetting(rootCtx.settings.outputDir, new PlainDirectory(Directory(outDir)))
           .setSetting(rootCtx.settings.XnoEnrichErrorMessages, !verbose)
           .setSetting(rootCtx.settings.help, verbose)
           .setSetting(rootCtx.settings.XshowPhases, verbose)
           .setSetting(rootCtx.settings.Vhelp, verbose)
           .setSetting(rootCtx.settings.Vprofile, verbose)
           .setSetting(rootCtx.settings.explain, verbose)
      }

      if (doCompile(newCompiler, toCompile).hasErrors) {
        val msgAddonMaybe = if (verbose) "" else " - try `--verbose` for more output"
        throw new CompilerError(s"Errors encountered during compilation$msgAddonMaybe") with NoStackTrace
      } else {
        fun(ctx0, outDir)
      }
    }
  }

  private def createReporter(linesBeforeRunBeforeCode: Int, linesBeforeScript: Int, originalReporter: Reporter): Reporter = {
    new Reporter {
      override def doReport(dia: Diagnostic)(using Context): Unit = {
        val adjustedPos = new SourcePosition(source = dia.pos.source, span = dia.pos.span, outer = dia.pos.outer) {
          override def line: Int = {
            val original = super.line
            val adjusted = original - linesBeforeScript
            if (adjusted >= 0) {
              adjusted
            } else {
              // adjusted line number is negative, i.e. the error must be in the `runBefore` code
              original - linesBeforeRunBeforeCode
            }
          }
        }
        originalReporter.doReport(new Diagnostic(dia.msg, adjustedPos, dia.level))
      }
    }
  }
}
