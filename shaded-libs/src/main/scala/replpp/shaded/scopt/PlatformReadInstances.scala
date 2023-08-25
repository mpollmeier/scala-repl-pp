package replpp.shaded.scopt

import java.net.{ URL, UnknownHostException }
import collection.{ Seq => CSeq }
import scala.io.Source

private[scopt] object platform {
  val _NL = System.getProperty("line.separator")

  import java.util.{ Locale, Calendar, GregorianCalendar }
  import java.text.SimpleDateFormat
  import java.io.File
  import java.nio.file.{ Path, Paths }
  import java.net.{ InetAddress, URI }

  type ParseException = java.text.ParseException
  def mkParseEx(s: String, p: Int) = new java.text.ParseException(s, p)

  trait PlatformReadInstances {
    def calendarRead(pattern: String): Read[Calendar] = calendarRead(pattern, Locale.getDefault)
    def calendarRead(pattern: String, locale: Locale): Read[Calendar] =
      Read.reads { s =>
        val fmt = new SimpleDateFormat(pattern, locale)
        val c = new GregorianCalendar(locale)
        c.setTime(fmt.parse(s))
        c
      }

    implicit val yyyymmdddRead: Read[Calendar] = calendarRead("yyyy-MM-dd")
    implicit val fileRead: Read[File] = Read.reads { new File(_) }
    implicit val pathRead: Read[Path] = Read.reads { Paths.get(_) }
    implicit val sourceRead: Read[Source] = Read.reads { Source.fromFile(_) }
    implicit val inetAddress: Read[InetAddress] = Read.reads { InetAddress.getByName(_) }
    implicit val urlRead: Read[URL] = Read.reads { new URL(_) }
  }

  def applyArgumentExHandler[C](
      desc: String,
      arg: String
  ): PartialFunction[Throwable, Either[CSeq[String], C]] = {
    case e: NumberFormatException =>
      Left(List(desc + " expects a number but was given '" + arg + "'"))
    case e: UnknownHostException =>
      Left(
        List(
          desc + " expects a host name or an IP address but was given '" + arg + "' which is invalid"
        )
      )
    case e: ParseException if e.getMessage.contains("Unparseable date") =>
      Left(
        List(
          s"$desc date format ('$arg') couldn't be parsed using implicit instance of `Read[Date]`."
        )
      )
    case _: ParseException =>
      Left(List(s"$desc expects a Scala duration but was given '$arg'"))
    case e: Throwable => Left(List(desc + " failed when given '" + arg + "'. " + e.getMessage))
  }

  def exit(status: Int): Nothing = sys.exit(status)
}
