package replpp.shaded.pprint
import replpp.shaded.fansi

object StringPrefix{
  def apply(i: Iterable[_]) =
    scala.collection.internal.pprint.CollectionName.get(i)
}
