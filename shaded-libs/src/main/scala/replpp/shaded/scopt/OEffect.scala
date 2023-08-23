package replpp.shaded.scopt

sealed trait OEffect
object OEffect {
  case class DisplayToOut(msg: String) extends OEffect
  case class DisplayToErr(msg: String) extends OEffect
  case class ReportError(msg: String) extends OEffect
  case class ReportWarning(msg: String) extends OEffect
  case class Terminate(exitState: Either[String, Unit]) extends OEffect
}

trait OEffectSetup {
  def displayToOut(msg: String): Unit
  def displayToErr(msg: String): Unit
  def reportError(msg: String): Unit
  def reportWarning(msg: String): Unit
  def terminate(exitState: Either[String, Unit]): Unit
}

abstract class DefaultOEffectSetup extends OEffectSetup {
  override def displayToOut(msg: String): Unit = {
    Console.out.println(msg)
  }
  override def displayToErr(msg: String): Unit = {
    Console.err.println(msg)
  }
  override def reportError(msg: String): Unit = {
    displayToErr("Error: " + msg)
  }
  override def reportWarning(msg: String): Unit = {
    displayToErr("Warning: " + msg)
  }
  override def terminate(exitState: Either[String, Unit]): Unit =
    exitState match {
      case Left(_)  => platform.exit(1)
      case Right(_) => platform.exit(0)
    }
}
