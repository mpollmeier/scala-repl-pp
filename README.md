## scala-repl-pp
Scala REPL PlusPlus - a (slightly) better Scala 3 / dotty REPL.

Note: currently this depends on a [slightly patched](https://github.com/mpollmeier/dotty/tree/michael/extensible-repl-minified) version of dotty. I'll try to get those merged upstream.

scala-repl-pp fills a gap between the standard Scala3 REPL, Ammonite and scala-cli:

### Why use scala-repl-pp over the regular Scala REPL?
* add runtime dependencies on startup with maven coordinates - automatically handles all downstream dependencies via [coursier](https://get-coursier.io/)
* pretty printing via [pprint](https://com-lihaoyi.github.io/PPrint/)
* define your own greeting and prompt
* @main with named arguments (regular Scala REPL only allows an argument list)
* supports predef scripts, i.e. run custom code before starting the REPL
* server mode: REPL runs embedded

### Why use scala-repl-pp over [Ammonite](http://ammonite.io/)?
* Ammonite's Scala 3 support is far from complete - e.g. autocompletion for extension methods has [many shortcomings](https://github.com/com-lihaoyi/Ammonite/issues/1297)
* far shorter and less complex dependency tree: Ammonite has some Scala2 dependencies intermixed, leading to downstream build problems like [this](https://github.com/com-lihaoyi/Ammonite/issues/1241)
* note: ammonite allows to add dependencies dynamically even in the middle of the REPL session - that's not supported by scala-repl-pp yet. You need to know which dependencies you want on startup. 

### Why use scala-repl-pp over [scala-cli](https://scala-cli.virtuslab.org/)?
* for context: scala-cli is mostly a wrapper around the regular Scala REPL and Ammonite, along with 
* TODO complete analysis
  * does it support @main named arguments?
  * can one add a dependency on it?

## Use Cases

Prerequisite (for now):
```bash
sbt stage
cd target/universal/stage/bin/
```

### REPL
```
./scala-repl-pp --help
./scala-repl-pp --prompt=myprompt --greeting='hey there!' --onExitCode='println("see ya!")'

./scala-repl-pp --predefCode='def foo = 42'
scala> foo
val res0: Int = 42

./scala-repl-pp --dependency com.michaelpollmeier:versionsort:1.0.7
scala> versionsort.VersionHelper.compare("1.0", "0.9")
val res0: Int = 1
```

### Scripting

Simple "Hello world" script: test-simple.sc
```scala
println("Hello!")
```

```bash
./scala-repl-pp --script test-simple.sc
```


Dependencies can be added via `//> using` syntax as in scala-cli: test-dependencies.sc
```scala
//> using com.michaelpollmeier:versionsort:1.0.7

val compareResult = versionsort.VersionHelper.compare("1.0", "0.9")
assert(compareResult == 1,
       s"result of comparison should be `1`, but was `$compareResult`")
```

```bash
./scala-repl-pp --script test-dependencies.sc
```


@main entrypoints: test-main.sc
```scala
@main def main() = println("Hello, world!")
```

```bash
./scala-repl-pp --script test-main.sc
```


@main entrypoint with named parameters: test-main-withargs.sc
```scala
@main def main(name: String) = {
  println(s"Hello, $name!")
}
```

```bash
./scala-repl-pp --script test-main-withargs.sc --params name=Michael
```



multiple @main entrypoints: test-main-multiple.sc
```scala
@main def foo() = println("foo!")
@main def bar() = println("bar!")
```

```bash
./scala-repl-pp --script test-main-multiple.sc --command=foo
```


