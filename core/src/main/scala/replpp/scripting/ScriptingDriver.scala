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
import scala.util.control.NonFatal
import scala.util.{Failure, Try}

/**
  * Runs a given script on the current JVM.
  *
  * Similar to dotty.tools.scripting.ScriptingDriver, but simpler and faster.
  * Main difference: we don't (need to) recursively look for main method entry points in the entire classpath,
  * because we have a fixed class and method name that ScriptRunner uses when it embeds the script and predef code.
  * */
class ScriptingDriver(compilerArgs: Array[String], predefFiles: Seq[Path], scriptFile: Path, scriptArgs: Array[String], verbose: Boolean) {
  private val wrappingResult = WrapForMainArgs(Files.readString(scriptFile))
  private val wrappedScript = Files.createTempFile("wrapped-script", ".sc")
  private val tempFiles = Seq.newBuilder[Path]
  private var executed = false

  Files.writeString(wrappedScript, wrappingResult.fullScript)
  tempFiles += wrappedScript

  if (verbose) {
    println(s"predefFiles: ${predefFiles.mkString(";")}")
    println(s"full script content (including wrapper code) ($wrappedScript)")
    println(wrappingResult.fullScript)
    println(s"script arguments: ${scriptArgs.mkString(",")}")
    println(s"compiler arguments: ${compilerArgs.mkString(",")}")
  }

  def compileAndRun(): Try[Unit] = {
    assert(!executed, "scripting driver can only be used once, and this instance has already been used.")
    executed = true
    val inputFiles = (wrappedScript +: predefFiles).filter(Files.exists(_))
    try {
      new SimpleDriver(lineNumberReportingAdjustment = -wrappingResult.linesBeforeWrappedCode)
        .compile(compilerArgs, inputFiles, verbose) { (ctx, outDir) =>
          given Context = ctx
          tempFiles += outDir

          val inheritedClasspath = ctx.settings.classpath.value
          val classpathEntries = ClassPath.expandPath(inheritedClasspath, expandStar = true).map(Paths.get(_))
          val mainMethod = lookupMainMethod(outDir, classpathEntries)
          mainMethod.invoke(null, scriptArgs)
        }
    } catch {
      case NonFatal(e) => Failure(e)
    } finally {
      tempFiles.result().foreach(deleteRecursively)
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
case class CompilerError(msg: String) extends RuntimeException(msg)
