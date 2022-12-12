## scala-repl-pp
Scala REPL PlusPlus - a (slightly) better Scala 3 / dotty REPL.
Note: this currently depends on a [slightly patched](https://github.com/mpollmeier/dotty/tree/michael/extensible-repl-minified) version of dotty. I'll try to get those merged upstream.

Motivation: scala-repl-pp fills a gap between the standard Scala3 REPL, Ammonite and scala-cli.

Note: this currently depends on a dotty fork, which has since been [merged](https://github.com/lampepfl/dotty/pull/16276) into dotty upstream, i.e. we'll be able to depend on the regular dotty release from 3.2.2 on :tada:

### Why use scala-repl-pp over the regular Scala REPL?
* add runtime dependencies on startup with maven coordinates - automatically handles all downstream dependencies via [coursier](https://get-coursier.io/)
* pretty printing via [pprint](https://com-lihaoyi.github.io/PPrint/)
* customize greeting, prompt and shutdown code
* multiple @main with named arguments (regular Scala REPL only allows an argument list)
* predef code - i.e. run custom code before starting the REPL - via string and scripts
* server mode: REPL runs embedded
* easily embeddable into your own build

### Why use scala-repl-pp over [Ammonite](http://ammonite.io/)?
* Ammonite's Scala 3 support is far from complete - e.g. autocompletion for extension methods has [many shortcomings](https://github.com/com-lihaoyi/Ammonite/issues/1297). In comparison: scala-repl-pp uses the regular Scala3/dotty ReplDriver. 
* Ammonite has some Scala2 dependencies intermixed, leading to downstream build problems like [this](https://github.com/com-lihaoyi/Ammonite/issues/1241). It's no longer easy to embed Ammonite into your own build.
* Note: Ammonite allows to add dependencies dynamically even in the middle of the REPL session - that's not supported by scala-repl-pp yet. You need to know which dependencies you want on startup. 

### Why use scala-repl-pp over [scala-cli](https://scala-cli.virtuslab.org/)?
scala-cli is mostly a wrapper around the regular Scala REPL and Ammonite, so depending on which one you choose, you essentially end up with the same differences as above. 

## Use Cases

Prerequisite (for now):
```bash
sbt stage
```

Generally speaking, `--help` is your friend!
```
./scala-repl-pp --help
```

### REPL
```
./scala-repl-pp --prompt=myprompt --greeting='hey there!' --onExitCode='println("see ya!")'

./scala-repl-pp --predefCode='def foo = 42'
scala> foo
val res0: Int = 42

./scala-repl-pp --dependency com.michaelpollmeier:versionsort:1.0.7
scala> versionsort.VersionHelper.compare("1.0", "0.9")
val res0: Int = 1
```

### Scripting

#### Simple "Hello world" script
test-simple.sc
```scala
println("Hello!")
```

```bash
./scala-repl-pp --script test-simple.sc
```

#### Predef code
test-predef.sc
```scala
println(foo)
```

```bash
./scala-repl-pp --script test-predef.sc --predefCode 'val foo = "Hello, predef!"'
```


#### Predef file(s)
test-predef.sc
```scala
println(foo)
```

test-predef-file.sc
```scala
val foo = "Hello, predef file"
```

```bash
./scala-repl-pp --script test-predef.sc --timesiles test-predef-file.sc
```

#### Importing files / scripts
foo.sc:
```scala
val foo = 42
```

test.sc:
```scala
//> using file foo.sc
println(foo)
```

```bash
./scala-repl-pp --script test.sc
```

#### Dependencies
Dependencies can be added via `//> using lib` syntax (like in scala-cli).

test-dependencies.sc:
```scala
//> using lib com.michaelpollmeier:versionsort:1.0.7

val compareResult = versionsort.VersionHelper.compare("1.0", "0.9")
assert(compareResult == 1,
       s"result of comparison should be `1`, but was `$compareResult`")
```

```bash
./scala-repl-pp --script test-dependencies.sc
```

Note: this also works with `using` directives in your predef code - for script and REPL mode.

#### @main entrypoints
test-main.sc
```scala
@main def main() = println("Hello, world!")
```

```bash
./scala-repl-pp --script test-main.sc
```

#### multiple @main entrypoints: test-main-multiple.sc
```scala
@main def foo() = println("foo!")
@main def bar() = println("bar!")
```

```bash
./scala-repl-pp --script test-main-multiple.sc --command=foo
```

#### named parameters
test-main-withargs.sc
```scala
@main def main(name: String) = {
  println(s"Hello, $name!")
}
```

```bash
./scala-repl-pp --script test-main-withargs.sc --params name=Michael
```

### Server
```bash
./scala-repl-pp --server

curl http://localhost:8080/query-sync -X POST -d '{"query": "val foo = 42"}'
curl http://localhost:8080/query-sync -X POST -d '{"query": "val bar = foo + 1"}'
```

### Embed into your own project
Try out the working [string calculator example](src/test/resources/demo-project) in this repo:
```bash
cd src/test/resources/demo-project
sbt stage
target/universal/stage/bin/stringcalc


Welcome to the magical world of string calculation!
Type `help` for help

stringcalc> add(One, Two)
val res0: stringcalc.Number = Number(3)
```

### Why are script line numbers incorrect?
Scala-REPL-PP currently uses a simplistic model for predef code|files and additionally imported files, and just copies everything into one large script. That simplicity naturally comes with a few limitations, e.g. line numbers may be different from the input script(s). 

A better approach would be to work with a separate compiler phase, similar to what Ammonite does. That way, we could inject all previously defined values|imports|... into the compiler, and extract all results from the compiler context. 

If there's a compilation issue, the temporary script file will not be deleted and the error output will tell you it's path, in order to help with debugging.
