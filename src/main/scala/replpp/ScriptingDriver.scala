package replpp

import scala.language.unsafeNulls
import java.nio.file.{Files, Path, Paths}
import java.io.File
import java.net.URLClassLoader
import java.lang.reflect.{Method, Modifier}
import dotty.tools.dotc.Driver
import dotty.tools.dotc.core.Contexts
import Contexts.{Context, ctx}
import dotty.tools.io.{ClassPath, Directory, PlainDirectory}

/** Copied and adapted from dotty.tools.scripting.ScriptingDriver
 * In other words: if anything in here is unsound, ugly or buggy: chances are that it was like that before... :) */
class ScriptingDriver(compilerArgs: Array[String], scriptFile: File, scriptArgs: Array[String]) extends Driver {

  def compileAndRun(pack: (Path, Seq[Path], String) => Boolean = null): Option[Throwable] = {
    val outDir = os.temp.dir(prefix = "scala3-scripting")

    setup(compilerArgs :+ scriptFile.getAbsolutePath, initCtx.fresh) match {
      case Some((toCompile, rootCtx)) =>
        given Context = rootCtx.fresh.setSetting(rootCtx.settings.outputDir,
          new PlainDirectory(Directory(outDir.toNIO)))

        if doCompile(newCompiler, toCompile).hasErrors then
          Some(ScriptingException("Errors encountered during compilation"))
        else {
          try {
            val classpath = s"${ctx.settings.classpath.value}$pathsep${sys.props("java.class.path")}"
            val classpathEntries: Seq[Path] = ClassPath.expandPath(classpath, expandStar = true).map(Paths.get(_))
            // TODO use shared constants
            // TODO use replpp namespace to avoid clashes
            val mainClass = "Main"
            val mainMethod = detectMainClassAndMethod(outDir.toNIO, classpathEntries)
            val invokeMain: Boolean = Option(pack).map { func =>
              func(outDir.toNIO, classpathEntries, mainClass)
            }.getOrElse(true)
            if invokeMain then mainMethod.invoke(null, scriptArgs)
            None
          } catch {
            case e: java.lang.reflect.InvocationTargetException => Some(e.getCause)
          } finally os.remove.all(outDir)
        }
      case None => None
    }
  }


  def detectMainClassAndMethod(outDir: Path, classpathEntries: Seq[Path]): Method = {
    val classpathUrls = (classpathEntries :+ outDir).map(_.toUri.toURL)
    val cl = URLClassLoader(classpathUrls.toArray)

    def collectMainMethods(target: File, path: String): List[Method] =
      val nameWithoutExtension = target.getName.takeWhile(_ != '.')
      val targetPath =
        if path.nonEmpty then s"$path.$nameWithoutExtension"
        else nameWithoutExtension

      if target.isDirectory then
        for
          packageMember <- target.listFiles.toList
          membersMainMethod <- collectMainMethods(packageMember, targetPath)
        yield membersMainMethod
      else if target.getName.endsWith(".class") then
        val cls = cl.loadClass(targetPath)
        try
          val method = cls.getMethod("main", classOf[Array[String]])
          if Modifier.isStatic(method.getModifiers) then List(method) else Nil
        catch
          case _: java.lang.NoSuchMethodException => Nil
      else Nil
    end collectMainMethods

    val mains = for
      file <- outDir.toFile.listFiles.toList
      method <- collectMainMethods(file, "")
    yield method

    mains.head
  }

  def pathsep = sys.props("path.separator")


}

case class ScriptingException(msg: String) extends RuntimeException(msg)
