package replpp.shaded.scopt

import scala.collection.{ Seq => CSeq }

object Validation {
  def validateValue[A](
      vs: CSeq[A => Either[String, Unit]]
  )(value: A): Either[CSeq[String], Unit] = {
    val results = vs map { _.apply(value) }
    results.foldLeft(OptionDef.makeSuccess[CSeq[String]]) { (acc, r) =>
      (acc match {
        case Right(_) => List[String]()
        case Left(xs) => xs
      }) ++ (r match {
        case Right(_) => List[String]()
        case Left(x)  => List[String](x)
      }) match {
        case CSeq() => acc
        case xs     => Left(xs)
      }
    }
  }
}
