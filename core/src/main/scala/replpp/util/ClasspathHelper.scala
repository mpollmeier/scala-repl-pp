package replpp
package util

import replpp.Config

import java.net.URL
import java.nio.file.Path
import scala.io.Source
import scala.util.Using

object ClasspathHelper {

  /**
   * Concatenates the classpath from multiple sources, each are required for different scenarios:
   * - `java.class.path` system property
   * - dependency artifacts as passed via (command-line) configuration
   * - jars from current class loader (recursively)
   *
   * To have reproducable results, we order the classpath entries. This may interfere with the user's deliberate choice
   * of order, but since we need to concatenate the classpath from different sources, the user can't really depend on
   * the order anyway.
   */
  def build(config: Config, quiet: Boolean = false): String = {
    val entries = Seq.newBuilder[String]
    System.getProperty("java.class.path").split(pathSeparator).foreach(entries.addOne)

    val fromDependencies = dependencyArtifacts(config)
    fromDependencies.foreach(file => entries.addOne(file.toString))
    if (fromDependencies.nonEmpty && !quiet) {
      println(s"resolved dependencies - adding ${fromDependencies.size} artifact(s) to classpath - to list them, enable verbose mode")
      if (verboseEnabled(config)) fromDependencies.foreach(println)
    }

    jarsFromClassLoaderRecursively(classOf[replpp.ReplDriver].getClassLoader)
      .foreach(url => entries.addOne(url.getPath))

    /** Important: we absolutely have to make sure this ends with a `pathSeparator`.
     * Otherwise, the last entry is lost somewhere down the line. I'm still debugging where exactly, but it looks
     * like somewhere in dotty. Will report upstream once I figured it out, but for now it doesn't harm to add a
     * pathseparator at the end.
     */
    entries.result().distinct.sorted.mkString(pathSeparator, pathSeparator, pathSeparator)
  }

  private def dependencyArtifacts(config: Config): Seq[Path] = {
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
