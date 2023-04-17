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

    // TODO automagically read credentials file(s) and pass that information on to fetch/resolve/...
    // idea being: we can dynamically set the coursier.repositories jvm property - ideally we do this differently as well, but that's a separate concern
    // coursier-cli does all of that automatically, but it's only published as a binary apparently
    System.setProperty("coursier.repositories", "ivy2Local|central|sonatype:releases|jitpack|https://shiftleft.jfrog.io/shiftleft/libs-release-local")
//    println(Dependencies.resolve(Seq("io.shiftleft::common:0.3.109")).get)

//    val creds = CacheDefaults.credentials.head
//    println(CacheDefaults.location) // ~/.cache/coursier/v1

   // TODO next thoughts:
   // understand how auth works for this resolve mechanism
   // pass a `withCache` to fetch?

//    val creds2 = coursier.bootstrap.launcher.credentials.Credentials

    val results = coursier.Fetch()
      .withCache(FileCache()) // the default cache throws away the credentials... see PlatformCacheCompanion.scala
      .addDependencies(DependencyParser.dependency("io.shiftleft::common:0.3.109", defaultScalaVersion = "3").getOrElse(???))
      .run()

    results.foreach(println)
  }

}
