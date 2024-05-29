package replpp.util

import dotty.tools.dotc.Driver
import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.Contexts.{Context, ctx}
import dotty.tools.io.{ClassPath, Directory, PlainDirectory}
import replpp.scripting.CompilerError

import java.io.File.pathSeparator
import java.nio.file.{Files, Path, Paths}
import scala.language.unsafeNulls

/** Compiles input files to a temporary directory
 *
 * TODO: use a VirtualDirectory for the output - I didn't manage to find a good way to pass those to the
 * Context of the DottyReplDriver yet...
 * val virtualDirectory = new VirtualDirectory("(virtual)")
 * val cp = ClassPathFactory.newClassPath(virtualDirectory)
 *
 * TODO: allow caching, i.e. store hash of inputs? maybe dottyc does that already?
 */
class SimpleDriver extends Driver {

  def compile(compilerArgs: Array[String], inputFiles: Seq[Path], verbose: Boolean): Option[Seq[Path]] = {
    if (verbose) {
      println(s"compiler arguments: ${compilerArgs.mkString(",")}")
      println(s"inputFiles: ${inputFiles.mkString(";")}")
    }
    val inputFiles0 = inputFiles.map(_.toAbsolutePath.toString).toArray
    setup(compilerArgs ++ inputFiles0, initCtx.fresh).map { case (toCompile, rootCtx) =>
      val outDir = Files.createTempDirectory("scala-repl-pp")

      given Context = {
        val ctx = rootCtx.fresh.setSetting(rootCtx.settings.outputDir, new PlainDirectory(Directory(outDir)))
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
        val classpath = s"${outDir.toAbsolutePath}$pathSeparator${ctx.settings.classpath.value}"
        val classpathEntries: Seq[Path] = ClassPath.expandPath(classpath, expandStar = true).map(Paths.get(_))
        classpathEntries
      }
    }
  }
  
}
