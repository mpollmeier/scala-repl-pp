package replpp

import dotty.tools.dotc.reporting.{HideNonSensicalMessages, StoreReporter, UniqueMessagePositions}
import dotty.tools.repl.*

/** random code that I needed to copy over from dotty to make things work, usually because it was `private[repl]`
 */
private[replpp] object DottyRandomStuff {

  /** Create empty outer store reporter
   * copied from https://github.com/lampepfl/dotty/blob/3.3.0-RC5/compiler/src/dotty/tools/repl/package.scala#L6  */
  def newStoreReporter: StoreReporter = {
    new StoreReporter(null) with UniqueMessagePositions with HideNonSensicalMessages
  }

  /** copied from https://github.com/lampepfl/dotty/blob/3.3.0-RC5/compiler/src/dotty/tools/repl/ParseResult.scala#L130 */
  object ParseResult {
    val commands: List[(String, String => ParseResult)] = List(
      Quit.command -> (_ => Quit),
      Quit.alias -> (_ => Quit),
      Help.command -> (_ => Help),
      Reset.command -> (arg => Reset(arg)),
      Imports.command -> (_ => Imports),
      Load.command -> (arg => Load(arg)),
      TypeOf.command -> (arg => TypeOf(arg)),
      DocOf.command -> (arg => DocOf(arg)),
      Settings.command -> (arg => Settings(arg)),
    )
  }
}
