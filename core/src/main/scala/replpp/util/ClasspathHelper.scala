package replpp
package util

import replpp.shaded.coursier.core.Version

import java.io.ByteArrayInputStream
import java.io.File.pathSeparator
import java.net.URL
import java.nio.file.{Path, Paths}
import java.util.jar.Attributes.Name
import java.util.jar.Manifest
import scala.annotation.nowarn
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Using}

object ClasspathHelper {
  case class JarWithManifestInfo(jar: Path, organisation: Option[String], name: Option[String], version: Option[String])

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

  protected[util] def createAsSeq(config: Config, quiet: Boolean = false): Seq[Path] = {
    val entries = Seq.newBuilder[Path]
    System.getProperty("java.class.path").split(pathSeparator).map(Paths.get(_)).foreach(entries.addOne)

    val fromDependencies = dependencyArtifacts(config)
    fromDependencies.foreach(entries.addOne)
    if (fromDependencies.nonEmpty && !quiet) {
      println(s"resolved dependencies - adding ${fromDependencies.size} artifact(s) to classpath - to list them, enable verbose mode")
      if (verboseEnabled(config)) fromDependencies.foreach(println)
    }

    jarsFromClassLoaderRecursively(classOf[replpp.ReplDriver].getClassLoader)
      .foreach(url => entries.addOne(Paths.get(url.toURI)))

    onlyHighestVersionForEachJar(entries.result().map(versionInfoFromJar)).sorted
  }

  /**
   * Goal: for each organisation/name tuple: filter out anything but the highest version. This is to ensure we don't have jars with different versions on the classpath.
   * Problem: since we don't only get the jars via maven coordinates, but also inherit from existing classpath entries, all we have is a list of jars.
   * The best solution is probably to use separate classloaders for the REPL itself and the code that's executed within. Similar to what ammonite does..
   * In the interim, we use this hack as a semi-best-effort basis: group by the information in their manifests. */
  private[util] def onlyHighestVersionForEachJar(dependencyInfos: Seq[JarWithManifestInfo]): Seq[Path] = {
    val resultJars = Seq.newBuilder[Path]

    val (withName, withoutName) = dependencyInfos.partition(_.name.isDefined)
    // do not filter out any jars that don't have their name defined in their manifest
    resultJars ++= withoutName.map(_.jar)

    withName
      .groupMap(dep => (dep.organisation, dep.name))(dep => (dep.jar, Version(dep.version.getOrElse("0"))))
      .foreach { case ((organisation, name), jars) =>
        // choose the jar with the highest version
        val result = jars.maxBy { case (_, version) => version } match {
          case (jar, _) => jar
        }
        resultJars += result
        if (jars.size > 1) {
          println(s"found ${jars.size} alternatives for organisation=${organisation.getOrElse("")} and name=${name.getOrElse("")}")
          println(s"-> using the jar with the highest version: $result")
        }
    }
    resultJars.result()
  }

  private def versionInfoFromJar(jar: Path): JarWithManifestInfo = {
    readFileFromZip(jar, "META-INF/MANIFEST.MF") match {
      case Success(manifestBytes) =>
        val manifest = new Manifest(new ByteArrayInputStream(manifestBytes))
        versionInfoFromManifest(jar, manifest)
      case Failure(_) =>
        // error while trying to read manifest, e.g. since it doesn't exist
        JarWithManifestInfo(jar, organisation = None, name = None, version = None)
    }
  }

  @nowarn
  private def versionInfoFromManifest(jar: Path, manifest: Manifest): JarWithManifestInfo = {
    val entries = manifest.getMainAttributes.asScala
    JarWithManifestInfo(
      jar,
      organisation = entries.get(Name.IMPLEMENTATION_VENDOR).orElse(entries.get(Name.IMPLEMENTATION_VENDOR_ID)).map(_.toString),
      name = entries.get(Name.IMPLEMENTATION_TITLE).orElse(entries.get(new Name("Automatic-Module-Name"))).orElse(entries.get(new Name("Bundle-SymbolicName"))).map(_.toString),
      version = entries.get(Name.IMPLEMENTATION_VERSION).orElse(entries.get(new Name("Bundle-Version"))).map(_.toString)
    )
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
