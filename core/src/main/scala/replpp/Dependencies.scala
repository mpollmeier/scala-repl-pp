package replpp

import java.io.File
import java.net.URI
import java.nio.file.{Path, Paths}
import scala.util.{Failure, Success, Try}
import replpp.util.Cache
import replpp.shaded.os

import sys.process.Process

object Dependencies {

  private val CoursierJarDownloadUrl = new URI("https://github.com/coursier/launchers/raw/master/coursier")

  def resolve(coordinates: Seq[String], additionalRepositories: Seq[String] = Nil, verbose: Boolean = false): Try[Seq[Path]] = {
    // TODO handle additionalRepositories: Seq[String] = Nil

    val coursierJarPath = Cache.getOrDownload("coursier.jar", CoursierJarDownloadUrl)
    val command = Seq("java", "-jar", coursierJarPath.toAbsolutePath.toString, "fetch") ++ coordinates
    if (verbose) println(s"executing `${command.mkString(" ")}`")

    Try(os.proc(command).call()) match {
      case Success(commandResult) =>
        Success(commandResult.out.text().split(System.lineSeparator()).map(Paths.get(_)).toIndexedSeq)
      case Failure(exception) =>
        Failure(new AssertionError(s"${getClass.getName}: error while invoking `${command.mkString(" ")}`", exception))
    }
  }

//  private def parseRepositories(additionalRepositories: Seq[String]): Try[Seq[Repository]] = {
//    ???
//    Try {
//      val parseResults = RepositoryParser.repositories(additionalRepositories)
//      parseResults.either match {
//        case Right(res) => res
//        case Left(failures) =>
//          throw new AssertionError(s"error while trying to parse given repository coordinates: ${failures.mkString(",")}")
//      }
//    }
//  }

//  private def parseDependencies(coordinates: Seq[String]): Try[Seq[Dependency]] =
//    util.sequenceTry(coordinates.map(parseDependency))
//
//  private def parseDependency(coordinate: String): Try[Dependency] = {
//    lazy val errorGeneric = s"error while trying to parse the following dependency coordinate: `$coordinate`"
//    Try {
//      DependencyParser.dependency(coordinate, defaultScalaVersion = "3") match {
//        // I'd expect coursier to return a `Left(errorMsg)` here in all error scenarios, but it only does it in certain scenarios...
//        case Left(error) => throw new AssertionError(s"$errorGeneric: $error")
//        case Right(value) => value
//      }
//    }.recover {
//      case error => throw new AssertionError(errorGeneric, error)
//    }
//  }

}
