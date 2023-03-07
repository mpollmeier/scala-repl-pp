package replpp.scripting

import dotty.tools.dotc.Driver
import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.Contexts.{Context, ctx}
import dotty.tools.io.{ClassPath, Directory, PlainDirectory}
import replpp.pathSeparator
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

      // TODO enable debugging options only for `--verbose`
      val ctx = rootCtx.fresh
        .setSetting(rootCtx.settings.help, true)
        .setSetting(rootCtx.settings.XshowPhases, true)
        .setSetting(rootCtx.settings.Vhelp, true)
        .setSetting(rootCtx.settings.Vprofile, true)
        .setSetting(rootCtx.settings.explain, true)
        .setSetting(rootCtx.settings.outputDir, new PlainDirectory(Directory(outDir.toNIO)))
//        .setSetting(rootCtx.settings.classpath, "/home/mp/Projects/scala-repl-pp/lib-staged-works/com.michaelpollmeier.scala-repl-pp-all-0.0.34+4-b3cd989c+20230306-1016.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.michaelpollmeier.scala-repl-pp-0.0.34+4-b3cd989c+20230306-1016.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.michaelpollmeier.scala-repl-pp-server-0.0.34+4-b3cd989c+20230306-1016.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.scala-lang.scala3-library_3-3.3.0-RC3.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.scala-lang.scala3-compiler_3-3.3.0-RC3.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.lihaoyi.mainargs_3-0.3.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.lihaoyi.os-lib_3-0.8.1.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.lihaoyi.pprint_3-0.8.1.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.github.scopt.scopt_3-4.1.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/io.get-coursier.coursier_2.13-2.0.16.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.scala-lang.modules.scala-xml_3-2.1.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.lihaoyi.cask_3-0.8.3.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.scala-lang.scala-library-2.13.10.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.scala-lang.scala3-interfaces-3.3.0-RC3.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.scala-lang.tasty-core_3-3.3.0-RC3.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.scala-lang.modules.scala-asm-9.4.0-scala-1.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.scala-sbt.compiler-interface-1.3.5.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.jline.jline-reader-3.19.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.jline.jline-terminal-3.19.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.jline.jline-terminal-jna-3.19.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.scala-lang.modules.scala-collection-compat_3-2.8.1.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.lihaoyi.geny_3-0.7.1.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.lihaoyi.fansi_3-0.4.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.lihaoyi.sourcecode_3-0.3.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/io.get-coursier.coursier-core_2.13-2.0.16.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/io.get-coursier.coursier-cache_2.13-2.0.16.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.github.alexarchambault.argonaut-shapeless_6.2_2.13-1.2.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/io.undertow.undertow-core-2.2.3.Final.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.lihaoyi.upickle_3-1.6.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.lihaoyi.cask-util_3-0.8.3.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.google.protobuf.protobuf-java-3.7.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.scala-sbt.util-interface-1.3.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/net.java.dev.jna.jna-5.3.1.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/io.get-coursier.coursier-util_2.13-2.0.16.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/io.github.alexarchambault.concurrent-reference-hash-map-1.0.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/io.github.alexarchambault.windows-ansi.windows-ansi-0.0.3.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/io.argonaut.argonaut_2.13-6.2.5.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.chuusai.shapeless_2.13-2.3.3.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.jboss.logging.jboss-logging-3.4.1.Final.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.jboss.xnio.xnio-api-3.8.0.Final.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.jboss.xnio.xnio-nio-3.8.0.Final.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.jboss.threads.jboss-threads-3.1.0.Final.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.lihaoyi.ujson_3-1.6.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.lihaoyi.upack_3-1.6.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.lihaoyi.upickle-implicits_3-1.6.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.lihaoyi.castor_3-0.1.8.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.java-websocket.Java-WebSocket-1.5.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.fusesource.jansi.jansi-1.18.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.scala-lang.scala-reflect-2.13.1.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.wildfly.common.wildfly-common-1.5.2.Final.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.wildfly.client.wildfly-client-config-1.0.1.Final.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/com.lihaoyi.upickle-core_3-1.6.0.jar:/home/mp/Projects/scala-repl-pp/lib-staged-works/org.slf4j.slf4j-api-1.7.25.jar")

      given Context = ctx

      if (doCompile(newCompiler, toCompile).hasErrors) {
        val msgAddonMaybe = if (verbose) "" else " - try `--verbose` for more output"
        Some(ScriptingException(s"Errors encountered during compilation$msgAddonMaybe"))
      } else {
        val classpath = s"${outDir.toNIO.toAbsolutePath.toString}$pathSeparator${ctx.settings.classpath.value}"
        val classpathEntries = ClassPath.expandPath(classpath, expandStar = true).map(Paths.get(_))
        val mainMethod = lookupMainMethod(outDir.toNIO, classpathEntries)
        try {
          mainMethod.invoke(null, scriptArgs)
          None // i.e. no Throwable - this is the 'good case' in the Driver api
        } catch {
          case e: java.lang.reflect.InvocationTargetException => Some(e.getCause)
        } finally os.remove.all(outDir)
      }
    }
  }

  private def lookupMainMethod(outDir: Path, classpathEntries: Seq[Path]): Method = {
    val classpathUrls = (classpathEntries :+ outDir).map(_.toUri.toURL)
    val clazz = URLClassLoader(classpathUrls.toArray).loadClass(MainClassName)
    val mainMethod = clazz.getMethod(MainMethodName, classOf[Array[String]])
    println("mainMethod: " + mainMethod)

    val clFoo = URLClassLoader(classpathUrls.toArray).loadClass("scala.Predef$")
    println("XX " + clFoo)
//    println("XX: " + clazz.getMethod("scala.Predef$.println"))

    mainMethod
  }
}
object ScriptingDriver {
  val MainClassName  = "ScalaReplPP"
  val MainMethodName = "main"
}
case class ScriptingException(msg: String) extends RuntimeException(msg)
