package replpp
package util

import java.io.File.pathSeparator
import java.net.URL
import java.nio.file.{Path, Paths}
import scala.io.Source
import scala.util.Using

object ClasspathHelper {

  /**
   * Concatenates the classpath from multiple sources, each are required for different scenarios:
   * - `java.class.path` system property
   * - dependency artifacts as passed via (command-line) configuration
   * - jars from current class loader (recursively)
   *
   * To have reproducible results, we order the classpath entries. This may interfere with the user's deliberate choice
   * of order, but since we need to concatenate the classpath from different sources, the user can't really depend on
   * the order anyway.
   */
  def create(config: Config, quiet: Boolean = false): String = {
    /** Important: we absolutely have to make sure this starts and ends with a `pathSeparator`.
     * Otherwise, the last entry is lost somewhere down the line (I didn't find the exact location where things go
     * wrong, but it looked like somewhere in dotty 3.3.0). */
    createAsSeq(config, quiet).mkString(pathSeparator, pathSeparator, pathSeparator)
  }

  protected[util] def createAsSeq(config: Config, quiet: Boolean = false): Seq[Path] = {
    val entries = Seq.newBuilder[Path]

    val jarsToKeepFromInheritedClasspathRegex = Seq(
      "classes",
      ".*scala-repl-pp.*",
      ".*scala3-compiler_3.*",
      ".*scala3-interfaces-.*",
      ".*scala3-library_3.*",
      ".*scala-library.*",
      ".*tasty-core_3.*",
      ".*scala-asm.*"
    ).map(_.r)

    // add select entries from out inherited classpath to the resulting classpath
    def addToEntriesMaybe(path: Path): Unit = {
      val filename = path.getFileName.toString
      if (jarsToKeepFromInheritedClasspathRegex.exists(_.matches(filename))) {
        if (verboseEnabled(config) && !quiet) println(s"using jar from inherited classpath: $path")
        entries.addOne(path)
      }
//      else // only for manual debugging
//        println(s"X0 not adding: $filename")
    }
    System.getProperty("java.class.path").split(pathSeparator).map(Paths.get(_)).foreach(addToEntriesMaybe)
    jarsFromClassLoaderRecursively(classOf[replpp.ReplDriver].getClassLoader).map(url => Paths.get(url.toURI)).foreach(addToEntriesMaybe)

    val fromDependencies = dependencyArtifacts(config)
    fromDependencies.foreach(entries.addOne)
    if (fromDependencies.nonEmpty && !quiet) {
      println(s"resolved dependencies - adding ${fromDependencies.size} artifact(s) to classpath - to list them, enable verbose mode")
      if (verboseEnabled(config)) fromDependencies.foreach(println)
    }

    entries.result().sorted
  }

  private[util] def dependencyArtifacts(config: Config): Seq[Path] = {
    val scriptLines = config.scriptFile.map { path =>
      Using.resource(Source.fromFile(path.toFile))(_.getLines.toSeq)
    }.getOrElse(Seq.empty)
    val allLines = allPredefLines(config) ++ scriptLines

    val resolvers = config.resolvers ++ UsingDirectives.findResolvers(allLines)
    val allDependencies = config.dependencies ++ UsingDirectives.findDeclaredDependencies(allLines)
    Dependencies.resolve(allDependencies, resolvers, verboseEnabled(config)).get
  }

  private def jarsFromClassLoaderRecursively(classLoader: ClassLoader): Seq[URL] = {
    classLoader match {
      case cl: java.net.URLClassLoader =>
        jarsFromClassLoaderRecursively(cl.getParent) ++ cl.getURLs
      case _ =>
        Seq.empty
    }
  }

}
