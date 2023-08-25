package replpp

import replpp.shaded.os
import replpp.util.Cache

import java.net.URI
import java.nio.file.{Path, Paths}
import scala.util.{Failure, Success, Try}

object Dependencies {

  private val CoursierJarDownloadUrl = new URI("https://github.com/coursier/launchers/raw/master/coursier")

  def resolve(coordinates: Seq[String], additionalRepositories: Seq[String] = Nil, verbose: Boolean = false): Try[Seq[Path]] = {
    if (coordinates.isEmpty) {
      Try(Seq.empty)
    } else {
      resolve0(coordinates, additionalRepositories, verbose)
    }
  }

  private def resolve0(coordinates: Seq[String], additionalRepositories: Seq[String], verbose: Boolean): Try[Seq[Path]] = {
    val coursierJarPath = Cache.getOrDownload("coursier.jar", CoursierJarDownloadUrl)
    val repositoryArgs = additionalRepositories.flatMap { repo =>
      Seq("--repository", repo)
    }
    val command = Seq("java", "-jar", coursierJarPath.toAbsolutePath.toString, "fetch") ++ repositoryArgs ++ coordinates
    if (verbose) println(s"executing `${command.mkString(" ")}`")

    Try(os.proc(command).call()) match {
      case Success(commandResult) =>
        Success(commandResult.out.text().split(System.lineSeparator()).map(Paths.get(_)).toIndexedSeq)
      case Failure(exception) =>
        Failure(new AssertionError(s"${getClass.getName}: error while invoking `${command.mkString(" ")}`", exception))
    }
  }

}
