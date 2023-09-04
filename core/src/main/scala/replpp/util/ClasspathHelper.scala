package replpp
package util

import replpp.Config

import java.net.URL
import java.io.File.pathSeparator
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
  def create(config: Config, quiet: Boolean = false): String = {
    /** Important: we absolutely have to make sure this starts and ends with a `pathSeparator`.
     * Otherwise, the last entry is lost somewhere down the line (I didn't find the exact location where things go
     * wrong, but it looked like somewhere in dotty 3.3.0). */
    createAsSeq(config, quiet).mkString(pathSeparator, pathSeparator, pathSeparator)
  }

  protected[util] def createAsSeq(config: Config, quiet: Boolean = false): Seq[String] = {
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

    onlyHighestVersionForEachJar(entries.result()).sorted
  }

  /** for each organisation/name tuple: filter out anything but the highest version */
  private[util] def onlyHighestVersionForEachJar(entries: Seq[String]): Seq[String] = ???

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
