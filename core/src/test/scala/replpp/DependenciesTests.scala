package replpp

/** only to be run manually for now, sorry... */
object DependenciesTests {

  def main(args: Array[String]): Unit = {
    // verify that we can access an artifact that's only available on a separate, password-protected repository
    // note: relies on the local ~/config/coursier/credentials.properties
    // and the private jfrog artifactory in shiftleft.jfrog.io
    // and the env var `COURSIER_REPOSITORIES=ivy2Local|central|sonatype:releases|jitpack|https://shiftleft.jfrog.io/shiftleft/libs-release-local`

    println(Dependencies.resolve(Seq("io.shiftleft::common:0.3.109")).get)
  }

}
