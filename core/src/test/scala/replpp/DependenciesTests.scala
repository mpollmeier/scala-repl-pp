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

    println(
      Dependencies.resolve(
//        Seq("io.shiftleftcommon0.3.109"), // TODO should print nice error msg
        Seq("io.shiftleft::common:0.3.109"),
        Seq("https://shiftleft.jfrog.io/shiftleft/libs-release-local")
      ).get
    )
  }

}
