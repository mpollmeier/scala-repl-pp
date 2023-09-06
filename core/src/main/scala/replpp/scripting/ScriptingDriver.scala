package replpp.scripting

import dotty.tools.dotc.Driver
import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.Contexts.{Context, ctx}
import dotty.tools.io.{ClassPath, Directory, PlainDirectory}
import replpp.util.deleteRecursively
import replpp.scripting.ScriptingDriver.*

import java.io.File.pathSeparator
import java.lang.reflect.{Method, Modifier}
import java.net.URLClassLoader
import java.nio.file.{Files, Path, Paths}
import scala.language.unsafeNulls

/**
  * Runs a given script on the current JVM.
  *
  * Similar to dotty.tools.scripting.ScriptingDriver, but simpler and faster.
  * Main difference: we don't (need to) recursively look for main method entrypoints in the entire classpath,
  * because we have a fixed class and method name that ScriptRunner uses when it embeds the script and predef code.
  * */
class ScriptingDriver(compilerArgs: Array[String], scriptFile: Path, scriptArgs: Array[String], verbose: Boolean) extends Driver {

  if (verbose) {
    println(s"full script content (including wrapper code) -> $scriptFile:")
    println(Files.readString(scriptFile))
    println(s"script arguments: ${scriptArgs.mkString(",")}")
    println(s"compiler arguments: ${compilerArgs.mkString(",")}")
  }

  def compileAndRun(): Option[Throwable] = {
    setup(compilerArgs :+ scriptFile.toAbsolutePath.toString, initCtx.fresh).flatMap { case (toCompile, rootCtx) =>
      val outDir = Files.createTempDirectory("scala3-scripting")

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
        Some(ScriptingException(s"Errors encountered during compilation$msgAddonMaybe"))
      } else {
        val classpath = s"${outDir.toAbsolutePath}$pathSeparator${ctx.settings.classpath.value}"
        val classpathEntries = ClassPath.expandPath(classpath, expandStar = true).map(Paths.get(_))
        val mainMethod = lookupMainMethod(outDir, classpathEntries)
        try {
          mainMethod.invoke(null, scriptArgs)
          None // i.e. no Throwable - this is the 'good case' in the Driver api
        } catch {
          case e: java.lang.reflect.InvocationTargetException => Some(e.getCause)
        } finally deleteRecursively(outDir)
      }
    }
  }

  private def lookupMainMethod(outDir: Path, classpathEntries: Seq[Path]): Method = {
    val classpathUrls = (classpathEntries :+ outDir).map(_.toUri.toURL)
    val clazz = URLClassLoader(classpathUrls.toArray).loadClass(MainClassName)
    clazz.getMethod(MainMethodName, classOf[Array[String]])
  }
}
object ScriptingDriver {
  val MainClassName  = "ScalaReplPP"
  val MainMethodName = "main"
}
case class ScriptingException(msg: String) extends RuntimeException(msg)
