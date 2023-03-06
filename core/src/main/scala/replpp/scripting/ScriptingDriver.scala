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
class ScriptingDriver(compilerArgs: Array[String], scriptFile: File, scriptArgs: Array[String], verbose: Boolean) extends Driver {

  if (verbose) {
    println(s"full script content (including wrapper code) -> $scriptFile:")
    println(os.read(os.Path(scriptFile.getAbsolutePath)))
    println(s"script arguments: ${scriptArgs.mkString(",")}")
    println(s"compiler arguments: ${compilerArgs.mkString(",")}")
  }

  def compileAndRun(): Option[Throwable] = {
    setup(compilerArgs :+ scriptFile.getAbsolutePath, initCtx.fresh).flatMap { case (toCompile, rootCtx) =>
      val outDir = os.temp.dir(prefix = "scala3-scripting", deleteOnExit = false)

//      // TODO cleanup
//      val classpath0: String = rootCtx.settings.classpath.value(using rootCtx)
//      val classloaderUrls = classOf[replpp.ReplDriver].getClassLoader.asInstanceOf[java.net.URLClassLoader].getURLs.mkString(pathSeparator)
//      val classpath1 = s"$classpath0$pathSeparator$classloaderUrls$pathSeparator${sys.props("java.class.path")}"
////      println("XXX1 cp=" + classpath1)
//      val ctx = rootCtx.fresh
//        .setSetting(rootCtx.settings.outputDir, new PlainDirectory(Directory(outDir.toNIO)))
//        .setSetting(rootCtx.settings.classpath, classpath1)
//      given Context = ctx
      given Context = rootCtx.fresh.setSetting(rootCtx.settings.outputDir, new PlainDirectory(Directory(outDir.toNIO)))

      if doCompile(newCompiler, toCompile).hasErrors then
        Some(ScriptingException("Errors encountered during compilation"))
      else try {
//        val classpath = s"${ctx.settings.classpath.value}$pathSeparator${sys.props("java.class.path")}"
        val classpath1 = ctx.settings.classpath.value
        val classpathEntries = ClassPath.expandPath(classpath1, expandStar = true).map(Paths.get(_))
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
