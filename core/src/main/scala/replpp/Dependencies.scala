package replpp

import coursier.Dependency
import coursier.cache.FileCache
import coursier.core.Repository
import coursier.parse.{DependencyParser, RepositoryParser}

import java.io.File
import java.nio.file.Path
import scala.util.{Failure, Success, Try}

object Dependencies {

  def resolve(coordinates: Seq[String], additionalRepositories: Seq[String] = Nil): Try[Seq[File]] = {
    for {
      repositories <- parseRepositories(additionalRepositories)
      dependencies <- parseDependencies(coordinates)
    } yield {
      coursier.Fetch()
        .withCache(FileCache()) // the default cache throws away the credentials... see PlatformCacheCompanion.scala
        .addRepositories(repositories: _*)
        .addDependencies(dependencies: _*)
        .run()
    }
  }

  private def parseRepositories(additionalRepositories: Seq[String]): Try[Seq[Repository]] = {
    Try {
      val parseResults = RepositoryParser.repositories(additionalRepositories)
      if (parseResults.isSuccess) {
        parseResults.either.getOrElse(???)
      } else {
        val failures = parseResults.either.left.getOrElse(???)
        throw new AssertionError(s"error while trying to parse given repository coordinates: ${failures.mkString(",")}")
      }
    }
  }


  private def parseDependencies(coordinates: Seq[String]): Try[Seq[Dependency]] =
    util.sequenceTry(coordinates.map(parseDependency))

  private def parseDependency(coordinate: String): Try[Dependency] = {
    lazy val errorGeneric = s"error while trying to parse the following dependency coordinate: `$coordinate`"
    Try {
      DependencyParser.dependency(coordinate, defaultScalaVersion = "3") match {
        // I'd expect coursier to return a `Left(errorMsg)` here in all error scenarios, but it only does it in certain scenarios...
        case Left(error) => throw new AssertionError(s"$errorGeneric: $error")
        case Right(value) => value
      }
    }.recover {
      case error => throw new AssertionError(errorGeneric, error)
    }
  }

}
