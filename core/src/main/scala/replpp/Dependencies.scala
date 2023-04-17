package replpp

import coursier.Dependency
import coursier.core.Repository
import coursier.parse.DependencyParser

import java.io.File
import java.nio.file.Path
import scala.util.{Failure, Try}

object Dependencies {

  def resolve(coordinates: Seq[String]): Try[Seq[File]] = {
    val x = for {
      dependencies <- parse(coordinates)
//      repositories <- readRepositoryConfiguration()
    } yield {
      coursier.Fetch()
        .addDependencies(dependencies: _*)
//        .withCache()
//        .addRepositories(repositories: _*)
        .run()
    }

    x
  }

  private def readRepositoryConfiguration(): Try[Seq[Repository]] = {
    // TODO read coursier property file - default location etc.
    // TODO add credentials as well
    ???
  }

  private def parse(coordinates: Seq[String]): Try[Seq[Dependency]] = {
    val parseResults = coordinates.map(coordinate => DependencyParser.dependency(coordinate, defaultScalaVersion = "3"))
    val failures = parseResults.collect { case Left(errorMsg) => errorMsg }
    Try {
      if (failures.nonEmpty)
        throw new AssertionError(s"error while trying to parse the following dependency coordinates: ${failures.mkString(",")}")
      else {
        parseResults.collect { case Right(dependency) => dependency }
      }
    }
  }

}
