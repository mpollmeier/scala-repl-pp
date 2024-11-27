// ./srp --script test-scripts/runBeforeCode-main.sc --runBefore 'import Byte.MinValue'

@main def foo() = {
  // val x: Int = "a string" // uncomment to check line number reporting
  println(s"test-main2 $MinValue")
}

