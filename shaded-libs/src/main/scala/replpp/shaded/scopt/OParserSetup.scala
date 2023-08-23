package replpp.shaded.scopt

trait OParserSetup {
  def renderingMode: RenderingMode
  def errorOnUnknownArgument: Boolean

  /**
   * Show usage text on parse error.
   * Defaults to None, which displays the usage text if
   * --help option is not defined.
   */
  def showUsageOnError: Option[Boolean]

}

abstract class DefaultOParserSetup extends OParserSetup {
  override def renderingMode: RenderingMode = RenderingMode.TwoColumns
  override def errorOnUnknownArgument: Boolean = true
  override def showUsageOnError: Option[Boolean] = None
}
