package replpp

/**
 * originally imported from https://github.com/com-lihaoyi/Ammonite/blob/151446c55e763b3b8c0152226f617ad1a0d719d4/amm/compiler/src/main/scala/ammonite/compiler/CompilerUtil.scala
 */

object CompilerUtil {

  val ignoredSyms = Set(
    "package class-use",
    "object package-info",
    "class package-info"
  )
  val ignoredNames = Set(
    // Probably synthetic
    "<init>",
    "<clinit>",
    "$main",
    // Don't care about this
    "toString",
    "equals",
    "wait",
    "notify",
    "notifyAll",
    "synchronized",
    "hashCode",
    "getClass",
    "eq",
    "ne",
    "##",
    "==",
    "!=",
    "isInstanceOf",
    "asInstanceOf",
    // Behaves weird in 2.10.x, better to just ignore.
    "_"
  )

}
