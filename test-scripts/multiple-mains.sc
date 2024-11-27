// ./srp --script test-scripts/multiple-mains.sc --command foo

@main def foo() = {
  println("in foo")
}

@main def bar() = {
  println("in bar")
}

