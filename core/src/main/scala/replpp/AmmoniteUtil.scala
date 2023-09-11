package replpp

/**
 * originally imported from https://github.com/com-lihaoyi/Ammonite/blob/151446c55e763b3b8c0152226f617ad1a0d719d4/amm/util/src/main/scala/ammonite/util/Util.scala
 */

object AmmoniteUtil {

  def encodeScalaSourcePath(path: Seq[Name]) = path.map(_.backticked).mkString(".")

}
