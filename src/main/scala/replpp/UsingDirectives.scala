package replpp

import java.util.stream.Collectors
import scala.jdk.CollectionConverters._

object UsingDirectives {
  val LibDirective = "//> using lib "

  def findDeclaredDependencies(source: String): Seq[String] = {
    source
      .lines()
      .map(_.trim)
      .filter(_.startsWith(LibDirective))
      .map(_.drop(LibDirective.length).trim)
      .collect(Collectors.toList)
      .asScala
      .toSeq
  }

}
