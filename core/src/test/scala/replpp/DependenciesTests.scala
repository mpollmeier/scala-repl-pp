package replpp

import coursier.cache.{CacheDefaults, FileCache}
import coursier.credentials.Credentials
import coursier.parse.DependencyParser

/** only to be run manually for now, sorry... */
object DependenciesTests {

  def main(args: Array[String]): Unit = {
    // verify that we can access an artifact that's only available on a separate, password-protected repository
    // note: relies on the local ~/config/coursier/credentials.properties
    // and the private jfrog artifactory in shiftleft.jfrog.io
    // and the env var `COURSIER_REPOSITORIES=ivy2Local|central|sonatype:releases|jitpack|https://shiftleft.jfrog.io/shiftleft/libs-release-local`

    // TODO set the extra repo in a different way, i.e. not via side effecting system property
    // TODO set this via a config parameter rather than hardcoded
    // TODO update the above comment
//    System.setProperty("coursier.repositories", "ivy2Local|central|sonatype:releases|jitpack|https://shiftleft.jfrog.io/shiftleft/libs-release-local")
    println(Dependencies.resolve(Seq("io.shiftleft::common:0.3.109")).get)
  }

}
