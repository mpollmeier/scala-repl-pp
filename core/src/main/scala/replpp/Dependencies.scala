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
        throw new AssertionError(s"error while trying to parse the following repository coordinates: ${failures.mkString(",")}")
      }
    }
  }


  private def parseDependencies(coordinates: Seq[String]): Try[Seq[Dependency]] = {
    Try {
      val parseResults = coordinates.map(coordinate => DependencyParser.dependency(coordinate, defaultScalaVersion = "3"))
      val failures = parseResults.collect { case Left(errorMsg) => errorMsg }
      if (failures.isEmpty) {
        parseResults.collect { case Right(dependency) => dependency }
      } else {
        throw new AssertionError(s"error while trying to parse the following dependency coordinates: ${failures.mkString(",")}")
      }
    }
  }

}
