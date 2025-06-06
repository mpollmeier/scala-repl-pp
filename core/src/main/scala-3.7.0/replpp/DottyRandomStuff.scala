package replpp

import dotty.tools.dotc.reporting.{HideNonSensicalMessages, StoreReporter, UniqueMessagePositions}
import dotty.tools.repl.*

private[replpp] object DottyRandomStuff {

  /** Create empty outer store reporter - copied from
    * https://github.com/scala/scala3/blob/c92e20e6be2117365361abfd0b7e6cb72720d5db/compiler/src/dotty/tools/repl/package.scala#L7
    * only change: removed [private] classifier so we can access it...
    */
  def newStoreReporter: StoreReporter = {
    new StoreReporter(null) with UniqueMessagePositions with HideNonSensicalMessages
  }

  /** copied from https://github.com/scala/scala3/blob/3.7.1/compiler/src/dotty/tools/repl/ParseResult.scala#L162
    * only change: removed [private] classifier so we can access it...
    * alternatively we could use reflection...
    */
  object ParseResult {
    val commands: List[(String, String => ParseResult)] = List(
      Quit.command -> (_ => Quit),
      Quit.alias -> (_ => Quit),
      Help.command -> (_  => Help),
      Reset.command -> (arg  => Reset(arg)),
      Imports.command -> (_  => Imports),
      JarCmd.command -> (arg => JarCmd(arg)),
      KindOf.command -> (arg => KindOf(arg)),
      Load.command -> (arg => Load(arg)),
      Require.command -> (arg => Require(arg)),
      TypeOf.command -> (arg => TypeOf(arg)),
      DocOf.command -> (arg => DocOf(arg)),
      Settings.command -> (arg => Settings(arg)),
      Sh.command -> (arg => Sh(arg)),
      Silent.command -> (_ => Silent),
    )
  }
}
