[![Release](https://github.com/mpollmeier/scala-repl-pp/actions/workflows/release.yml/badge.svg)](https://github.com/mpollmeier/scala-repl-pp/actions/workflows/release.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.michaelpollmeier/scala-repl-pp_3/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.michaelpollmeier/scala-repl-pp_3)

## scala-repl-pp
Scala REPL PlusPlus - a (slightly) better Scala 3 / dotty REPL.

Motivation: scala-repl-pp fills a gap between the standard Scala3 REPL, Ammonite and scala-cli.

## TOC
<!-- generated with: -->
<!-- markdown-toc --maxdepth 3 README.md -->

- [Benefits over / comparison with](#benefits-over--comparison-with)
  * [Regular Scala REPL](#regular-scala-repl)
  * [Ammonite](#ammonite)
  * [scala-cli](#scala-cli)
- [Build it locally](#build-it-locally)
- [REPL](#repl)
  * [Add dependencies with maven coordinates](#add-dependencies-with-maven-coordinates)
  * [Importing additional script files interactively](#importing-additional-script-files-interactively)
- [Scripting](#scripting)
  * [Simple "Hello world" script](#simple-hello-world-script)
  * [Predef code for script](#predef-code-for-script)
  * [Predef code via environment variable](#predef-code-via-environment-variable)
  * [Predef file(s)](#predef-files)
  * [Importing files / scripts](#importing-files--scripts)
  * [Dependencies](#dependencies)
  * [@main entrypoints](#main-entrypoints)
  * [multiple @main entrypoints: test-main-multiple.sc](#multiple-main-entrypoints-test-main-multiplesc)
  * [named parameters](#named-parameters)
- [Server mode](#server-mode)
- [Embed into your own project](#embed-into-your-own-project)
- [Limitations](#limitations)
  * [Why are script line numbers incorrect?](#why-are-script-line-numbers-incorrect)

## Benefits over / comparison with

### Regular Scala REPL
* add runtime dependencies on startup with maven coordinates - automatically handles all downstream dependencies via [coursier](https://get-coursier.io/)
* pretty printing via [pprint](https://com-lihaoyi.github.io/PPrint/)
* customize greeting, prompt and shutdown code
* multiple @main with named arguments (regular Scala REPL only allows an argument list)
* predef code - i.e. run custom code before starting the REPL - via string and scripts
* server mode: REPL runs embedded
* easily embeddable into your own build

### [Ammonite](http://ammonite.io)
* Ammonite's Scala 3 support is far from complete - e.g. autocompletion for extension methods has [many shortcomings](https://github.com/com-lihaoyi/Ammonite/issues/1297). In comparison: scala-repl-pp uses the regular Scala3/dotty ReplDriver. 
* Ammonite has some Scala2 dependencies intermixed, leading to downstream build problems like [this](https://github.com/com-lihaoyi/Ammonite/issues/1241). It's no longer easy to embed Ammonite into your own build.
* Note: Ammonite allows to add dependencies dynamically even in the middle of the REPL session - that's not supported by scala-repl-pp yet. You need to know which dependencies you want on startup. 

### [scala-cli](https://scala-cli.virtuslab.org/)
scala-cli is mostly a wrapper around the regular Scala REPL and Ammonite, so depending on which one you choose, you essentially end up with the same differences as above. 

## Build it locally

Prerequisite for all of the below:
```bash
sbt stage
```

Generally speaking, `--help` is your friend!
```
./scala-repl-pp --help
```

## REPL

```bash
# run with defaults
./scala-repl-pp

# customize prompt, greeting and exit code
./scala-repl-pp --prompt myprompt --greeting 'hey there!' --onExitCode 'println("see ya!")'

# pass some predef code
./scala-repl-pp --predefCode 'def foo = 42'
scala> foo
val res0: Int = 42
```

### Add dependencies with maven coordinates
Note: the dependencies must be known at startup time, either via `--dependencies` parameter...
```
./scala-repl-pp --dependencies com.michaelpollmeier:versionsort:1.0.7
scala> versionsort.VersionHelper.compare("1.0", "0.9")
val res0: Int = 1
```
To add multiple dependencies, you can specify this parameter multiple times.

Alternatively, use the `//> using lib` directive in predef code or predef files:
```
echo '//> using lib com.michaelpollmeier:versionsort:1.0.7' > predef.sc

./scala-repl-pp --predefFiles predef.sc

scala> versionsort.VersionHelper.compare("1.0", "0.9")
val res0: Int = 1
```

### Importing additional script files interactively
```
echo 'val bar = foo' > myScript.sc

./scala-repl-pp

val foo = 1
//> using file myScript.sc
println(bar) //1
```

## Scripting

See [ScriptRunnerTest](core/src/test/scala/replpp/scripting/ScriptRunnerTest.scala) for a more complete and in-depth overview.

### Simple "Hello world" script
test-simple.sc
```scala
println("Hello!")
```

```bash
./scala-repl-pp --script test-simple.sc
```

### Predef code for script
test-predef.sc
```scala
println(foo)
```

```bash
./scala-repl-pp --script test-predef.sc --predefCode 'val foo = "Hello, predef!"'
```

### Predef code via environment variable
test-predef.sc
```scala
println(foo)
```

```bash
export SCALA_REPL_PP_PREDEF_CODE='val foo = "Hello, predef!"'
./scala-repl-pp --script test-predef.sc
```

### Predef file(s)
test-predef.sc
```scala
println(foo)
```

test-predef-file.sc
```scala
val foo = "Hello, predef file"
```

```bash
./scala-repl-pp --script test-predef.sc --predefFiles test-predef-file.sc
```
To import multiple scripts, you can specify this parameter multiple times.

### Importing files / scripts
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

### Dependencies
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

### @main entrypoints
test-main.sc
```scala
@main def main() = println("Hello, world!")
```

```bash
./scala-repl-pp --script test-main.sc
```

### multiple @main entrypoints: test-main-multiple.sc
```scala
@main def foo() = println("foo!")
@main def bar() = println("bar!")
```

```bash
./scala-repl-pp --script test-main-multiple.sc --command foo
```

### named parameters
test-main-withargs.sc
```scala
@main def main(name: String) = {
  println(s"Hello, $name!")
}
```

```bash
./scala-repl-pp --script test-main-withargs.sc --params name=Michael
```

## Server mode
```bash
./scala-repl-pp --server

curl http://localhost:8080/query-sync -X POST -d '{"query": "val foo = 42"}'
curl http://localhost:8080/query-sync -X POST -d '{"query": "val bar = foo + 1"}'
```

## Embed into your own project
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

## Predef code and scripts
There's a variety of ways to define predef code, i.e. code that is being run before any given script:

```
echo 'def bar = 90' > ~/.scala-repl-pp.sc
echo 'def baz = 91' > script1.sc
echo 'def bam = 92' > script2.sc
export SCALA_REPL_PP_PREDEF_CODE='def bax = 93'

./scala-repl-pp --predefCode='def foo = 42' --predefFiles script1.sc --predefFiles script2.sc

scala> foo
val res0: Int = 42

scala> bar
val res1: Int = 90

scala> baz
val res2: Int = 91

scala> bam
val res3: Int = 92

scala> bax
val res4: Int = 93
```

## Verbose mode
If verbose mode is enabled, you'll get additional information about classpaths and complete scripts etc. 
To enable it, you can either pass `--verbose` or set the environment variable `SCALA_REPL_PP_VERBOSE=true`.

## Limitations / Debugging

### Why are script line numbers incorrect?
Scala-REPL-PP currently uses a simplistic model for predef code|files and additionally imported files, and just copies everything into one large script. That simplicity naturally comes with a few limitations, e.g. line numbers may be different from the input script(s). 

A better approach would be to work with a separate compiler phase, similar to what Ammonite does. That way, we could inject all previously defined values|imports|... into the compiler, and extract all results from the compiler context. That's a goal for the future. 

If there's a compilation issue, the temporary script file will not be deleted and the error output will tell you it's path, in order to help with debugging.

