package stringcalc

trait Number(numeric: Int) {
  override def toString = s"Number($numeric)"
}
object One   extends Number(1)
object Two   extends Number(2)
object Three extends Number(3)

object StringCalculator {
  def add(number1: Number, number2: Number): Number =
    (number1, number2) match {
      case (One, One) => Two
      case (One, Two) => Three
      case (Two, One) => Three
      case _ => ???
    }
}
