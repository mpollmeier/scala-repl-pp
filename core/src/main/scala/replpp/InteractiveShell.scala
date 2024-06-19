package replpp

import dotty.tools.repl.{AbstractFileClassLoader, State}
import replpp.util.ClasspathHelper

import java.net.URLClassLoader
import scala.util.control.NoStackTrace

object InteractiveShell {

  def run(config: Config): Unit = {
    import config.colors

//    lazy val classpathEntries = ClasspathHelper.createAsURLs(config, quiet = true)
//    lazy val classLoader = URLClassLoader(classpathEntries.toArray)
    val predefClassLoaderMaybe: Option[ClassLoader] = precompilePredefFilesMaybe(config).map { virtualDir =>
//      AbstractFileClassLoader(virtualDir, parent = classLoader)
      AbstractFileClassLoader(virtualDir, parent = null)
    }
    println(s"XXX5 ${predefClassLoaderMaybe}")
//    predefClassLoaderMaybe.get.getName
    // slight problem: predef doesn't work any more... what's wrong? double check with how it was before...

    val compilerArgs = replpp.compilerArgs(config)
    // TODO current issue: definitions from predef can't get resolved. root cause: we need to pass them as compiler args, like before...
    val replDriver = new ReplDriver(
      compilerArgs,
      onExitCode = config.onExitCode,
      greeting = config.greeting,
      prompt = config.prompt.getOrElse("scala"),
      maxHeight = config.maxHeight,
//      classLoader = predefClassLoaderMaybe,
      classLoader = None,
    )

    val initialState: State = replDriver.initialState
    val predefCode = DefaultPredef
    val state: State = {
      if (verboseEnabled(config)) {
        println(s"compiler arguments: ${compilerArgs.mkString(",")}")
        println(predefCode)
        replDriver.run(predefCode)(using initialState)
      } else {
        replDriver.runQuietly(predefCode)(using initialState)
      }
    }

    if (predefCode.nonEmpty && state.objectIndex != 1) {
      throw new AssertionError(s"compilation error for predef code - error should have been reported above ^") with NoStackTrace
    }

    replDriver.runUntilQuit(using state)()
  }
  
}