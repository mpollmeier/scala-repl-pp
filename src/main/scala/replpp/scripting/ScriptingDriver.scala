package replpp.scripting

import dotty.tools.dotc.Driver
import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.Contexts.{Context, ctx}
import dotty.tools.io.{ClassPath, Directory, PlainDirectory}
import replpp.scripting.ScriptingDriver.*

import java.io.File
import java.lang.reflect.{Method, Modifier}
import java.net.URLClassLoader
import java.nio.file.{Files, Path, Paths}
import scala.language.unsafeNulls

/**
 * Similar to dotty.tools.scripting.ScriptingDriver, but simpler and faster.
 * Main difference: we don't (need to) recursive look for main method entrypoints in the entire classpath,
 * because we have a fixed class and method name that ScriptRunner uses when it embeds the script and predef code.
 * */
class ScriptingDriver(compilerArgs: Array[String], scriptFile: File, scriptArgs: Array[String]) extends Driver {

  def compileAndRun(): Option[Throwable] = {
    setup(compilerArgs :+ scriptFile.getAbsolutePath, initCtx.fresh).flatMap { case (toCompile, rootCtx) =>
      val outDir = os.temp.dir(prefix = "scala3-scripting", deleteOnExit = false)
      given Context = rootCtx.fresh.setSetting(rootCtx.settings.outputDir, new PlainDirectory(Directory(outDir.toNIO)))

      if doCompile(newCompiler, toCompile).hasErrors then
        Some(ScriptingException("Errors encountered during compilation"))
      else try {
        val classpath = s"${ctx.settings.classpath.value}$pathSeparator${sys.props("java.class.path")}"
        val classpathEntries = ClassPath.expandPath(classpath, expandStar = true).map(Paths.get(_))
        lookupMainMethod(outDir.toNIO, classpathEntries).invoke(null, scriptArgs)
        None
      } catch {
        case e: java.lang.reflect.InvocationTargetException => Some(e.getCause)
      } finally os.remove.all(outDir)
    }
  }

  private def lookupMainMethod(outDir: Path, classpathEntries: Seq[Path]): Method = {
    val classpathUrls = (classpathEntries :+ outDir).map(_.toUri.toURL)
    val clazz = URLClassLoader(classpathUrls.toArray).loadClass(MainClassName)
    clazz.getMethod(MainMethodName, classOf[Array[String]])
  }

  lazy val pathSeparator = sys.props("path.separator")
}
object ScriptingDriver {
  val MainClassName  = "ScalaReplPP"
  val MainMethodName = "main"
}
case class ScriptingException(msg: String) extends RuntimeException(msg)
