package replpp

import dotty.tools.dotc.Driver
import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.Contexts.{Context, ctx}
import dotty.tools.io.{ClassPath, Directory, PlainDirectory}
import replpp.ScriptingDriver.{MainClassName, MainMethodName}

import java.io.File
import java.lang.reflect.{Method, Modifier}
import java.net.URLClassLoader
import java.nio.file.{Files, Path, Paths}
import scala.language.unsafeNulls

/** Copied and adapted from dotty.tools.scripting.ScriptingDriver
 * In other words: if anything in here is unsound, ugly or buggy: chances are that it was like that before... :) */
class ScriptingDriver(compilerArgs: Array[String], scriptFile: File, scriptArgs: Array[String]) extends Driver {

  def compileAndRun(): Option[Throwable] = {
    val outDir = os.temp.dir(prefix = "scala3-scripting")

    setup(compilerArgs :+ scriptFile.getAbsolutePath, initCtx.fresh) match {
      case Some((toCompile, rootCtx)) =>
        given Context = rootCtx.fresh.setSetting(rootCtx.settings.outputDir,
          new PlainDirectory(Directory(outDir.toNIO)))

        if doCompile(newCompiler, toCompile).hasErrors then
          Some(ScriptingException("Errors encountered during compilation"))
        else {
          try {
            val classpath = s"${ctx.settings.classpath.value}$pathSeparator${sys.props("java.class.path")}"
            val classpathEntries = ClassPath.expandPath(classpath, expandStar = true).map(Paths.get(_))
            val mainMethod = lookupMainMethod(outDir.toNIO, classpathEntries)
            mainMethod.invoke(null, scriptArgs)
            None //TODO return a `Try[Unit]` instead?
          } catch {
            case e: java.lang.reflect.InvocationTargetException => Some(e.getCause)
          } finally os.remove.all(outDir)
        }
      case None => None
    }
  }

  private def lookupMainMethod(outDir: Path, classpathEntries: Seq[Path]): Method = {
    val classpathUrls = (classpathEntries :+ outDir).map(_.toUri.toURL)
    val cl = URLClassLoader(classpathUrls.toArray)
    val cls = cl.loadClass(MainClassName)
    cls.getMethod(MainMethodName, classOf[Array[String]])
  }

  lazy val pathSeparator = sys.props("path.separator")
}

object ScriptingDriver {
  val MainClassName  = "ScalaReplPP"
  val MainMethodName = "main"
}

case class ScriptingException(msg: String) extends RuntimeException(msg)
