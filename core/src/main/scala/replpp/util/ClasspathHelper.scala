package replpp
package util

import replpp.Config

import java.io.ByteArrayInputStream
import java.net.URL
import java.io.File.pathSeparator
import java.nio.file.{Path, Paths}
import java.util.Map.entry
import scala.annotation.nowarn
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

  // TODO drop
  def main(args: Array[String]): Unit = {
    import java.util.jar.Attributes.Name
    import java.util.jar.Manifest
    import scala.jdk.CollectionConverters.*

    val additionalDep = args.head
    createAsSeq(Config(dependencies = Seq(additionalDep))).foreach { jar =>
      //    val file = Paths.get("/home/mp/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/ammonite_2.13.0/2.5.8/ammonite_2.13.0-2.5.8.jar")
      val file = Paths.get(jar)
      val manifestBytes = readFileFromZip(file, "META-INF/MANIFEST.MF")
      val manifest = new Manifest(new ByteArrayInputStream(manifestBytes))
      val entries = manifest.getMainAttributes.asScala

      @nowarn
      val organisationMaybe = entries.get(Name.IMPLEMENTATION_VENDOR).orElse(entries.get(Name.IMPLEMENTATION_VENDOR_ID)).map(_.toString)
      val nameMaybe = entries.get(Name.IMPLEMENTATION_TITLE).orElse(entries.get(new Name("Automatic-Module-Name"))).orElse(entries.get(new Name("Bundle-SymbolicName"))).map(_.toString)
      val versionMaybe = entries.get(Name.IMPLEMENTATION_VERSION).orElse(entries.get(new Name("Bundle-Version"))).map(_.toString)

      case class JarWithManifestInfo(path: Path, organisation: Option[String], name: Option[String], version: Option[String])

      val ctx = JarWithManifestInfo(file, organisationMaybe, nameMaybe, versionMaybe)
      println(ctx)
    }

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

  /**
   * Goal: for each organisation/name tuple: filter out anything but the highest version. This is to ensure we don't have jars with different versions on the classpath.
   * Problem: since we don't only get the jars via maven coordinates, but also inherit from existing classpath entries, all we have is a list of jars.
   * The best solution is probably to use separate classloaders for the REPL itself and the code that's executed within. Similar to what ammonite does..
   * In the interim, we use this hack as a semi-best-effort basis: group by the information in their manifests.  */
  private[util] def onlyHighestVersionForEachJar(entries: Seq[String]): Seq[String] = {
//    ???
    entries
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
