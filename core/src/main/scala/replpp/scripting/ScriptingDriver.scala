package replpp.scripting

import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.io.ClassPath
import replpp.scripting.ScriptingDriver.*
import replpp.util.{SimpleDriver, deleteRecursively}

import java.lang.reflect.Method
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
class ScriptingDriver(compilerArgs: Array[String], predefFiles: Seq[Path], scriptFile: Path, scriptArgs: Array[String], verbose: Boolean) {
  if (verbose) {
    println(s"predefFiles: ${predefFiles.mkString(";")}")
    println(s"full script content (including wrapper code) -> $scriptFile:")
    println(Files.readString(scriptFile))
    println(s"script arguments: ${scriptArgs.mkString(",")}")
    println(s"compiler arguments: ${compilerArgs.mkString(",")}")
  }

  // TODO change return type to Try[A]?
  def compileAndRun(): Option[Throwable] = {
    val inputFiles = (scriptFile +: predefFiles)
    new SimpleDriver().compile(compilerArgs, inputFiles, verbose) { (ctx, outDir) =>
      given Context = ctx
      val inheritedClasspath = ctx.settings.classpath.value
      val classpathEntries = ClassPath.expandPath(inheritedClasspath, expandStar = true).map(Paths.get(_))
      val mainMethod = lookupMainMethod(outDir, classpathEntries)
      try {
        mainMethod.invoke(null, scriptArgs)
        None // i.e. no Throwable - this is the 'good case' in the Driver api
      } catch {
        case e: java.lang.reflect.InvocationTargetException => Some(e.getCause)
      } finally deleteRecursively(outDir)
    }.flatten
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
case class CompilerError(msg: String) extends RuntimeException(msg)
