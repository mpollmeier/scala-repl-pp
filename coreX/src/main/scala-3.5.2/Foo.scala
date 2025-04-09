import dotty.tools.repl

object Foo {
  val shared: Int = Shared.number
  val command1 = repl.Imports
  // val command2 = repl.Silent

  // val breaks: Int = "asd"
}
