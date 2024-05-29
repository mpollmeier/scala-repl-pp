package replpp

import dotty.tools.dotc.classpath.ClassPathFactory
import dotty.tools.io.VirtualDirectory
import dotty.tools.runner.ScalaClassLoader
import replpp.scripting.CompilerError

import java.nio.file.Path
import scala.util.Try

object InteractiveShell {

  def run(config: Config): Unit = {
    import config.colors

    // TODO step 1: compile given predef files (if any) to tasty, so we can then load it into the repl in step 2
    val compilationResults =
      new FooHelper.Driver0().compile(
        replpp.compilerArgs(config),
        inputFiles = Seq(Path.of("/home/mp/tmp/aaa.scala")),
        verbose = false
      ).get

    val compilerArgs = replpp.compilerArgs(config.withAdditionalClasspathEntries(compilationResults))

    if (verboseEnabled(config))
      println(s"compiler arguments: ${compilerArgs.mkString(",")}")

    new ReplDriver(
      compilerArgs,
      onExitCode = config.onExitCode,
      greeting = config.greeting,
      prompt = config.prompt.getOrElse("scala"),
      maxHeight = config.maxHeight
    ).runUntilQuit()
  }
}

// TODO refactor: integrate into InteractiveShell
object FooHelper {
  import dotty.tools.dotc.Driver
  import dotty.tools.dotc.core.Contexts
  import dotty.tools.dotc.core.Contexts.{Context, ctx}
  import dotty.tools.io.{ClassPath, Directory, PlainDirectory}
  import java.io.File.pathSeparator
  import java.nio.file.{Files, Path, Paths}
  import scala.language.unsafeNulls

  def main(args: Array[String]): Unit = {
    val config = Config(
      predefFiles = Seq(Path.of("/home/mp/tmp/aaa.scala"))
    )
    val compilerArgs = replpp.compilerArgs(config)

    /** TODO using a VirtualDirectory for the classes output would make a lot of sense, but I didn't manage to find a
     * good way to pass it to the Context of the DottyReplDriver yet...
     * val virtualDirectory = new VirtualDirectory("(virtual)")
     * val cp = ClassPathFactory.newClassPath(virtualDirectory)
     */

    println(new Driver0().compile(compilerArgs, config.predefFiles, config.verbose))
    // TODO allow to configure output directory, and/or get that here
    // TODO later: allow caching, i.e. store hash of inputs? maybe dottyc does that already?
  }

  class Driver0 extends Driver {

    def compile(compilerArgs: Array[String], inputFiles: Seq[Path], verbose: Boolean): Try[Seq[Path]] = {
      if (verbose) {
        println(s"compiler arguments: ${compilerArgs.mkString(",")}")
        println(s"inputFiles: ${inputFiles.mkString(";")}")
      }
      Try {
        val inputFiles0 = inputFiles.map(_.toAbsolutePath.toString).toArray
        setup(compilerArgs ++ inputFiles0, initCtx.fresh).flatMap { case (toCompile, rootCtx) =>
          val outDir = Files.createTempDirectory("scala-repl-pp-predef")

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
            Option(classpathEntries)
          }
        }.get
      }
    }
  }

}