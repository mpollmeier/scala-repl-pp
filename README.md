[![Release](https://github.com/mpollmeier/scala-repl-pp/actions/workflows/release.yml/badge.svg)](https://github.com/mpollmeier/scala-repl-pp/actions/workflows/release.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.michaelpollmeier/scala-repl-pp_3/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.michaelpollmeier/scala-repl-pp_3)

## scala-repl-pp
Scala REPL PlusPlus: a better Scala 3 REPL. With many features inspired by ammonite and scala-cli while keeping complexity low by depending on (and not adding much on top of) the stock Scala 3 REPL. 

This is (also) a breeding ground for improvements to the regular scala REPL: we're forking parts of the REPL to later bring the changes back into the dotty codebase (ideally).

Runs on JDK11+.

## TOC
<!-- generated with: -->
<!-- markdown-toc --maxdepth 3 README.md|tail -n +3 -->

- [Benefits over / comparison with](#benefits-over--comparison-with)
  * [Regular Scala REPL](#regular-scala-repl)
  * [Ammonite](#ammonite)
  * [scala-cli](#scala-cli)
- [Build it locally](#build-it-locally)
- [REPL](#repl)
  * [Add dependencies with maven coordinates](#add-dependencies-with-maven-coordinates)
  * [Importing additional script files interactively](#importing-additional-script-files-interactively)
  * [Rendering of output](#rendering-of-output)
- [Scripting](#scripting)
  * [Simple "Hello world" script](#simple-hello-world-script)
  * [Predef file(s) used in script](#predef-files-used-in-script)
  * [Importing files / scripts](#importing-files--scripts)
  * [Dependencies](#dependencies)
  * [@main entrypoints](#main-entrypoints)
  * [multiple @main entrypoints: test-main-multiple.sc](#multiple-main-entrypoints-test-main-multiplesc)
  * [named parameters](#named-parameters)
- [Additional dependency resolvers and credentials](#additional-dependency-resolvers-and-credentials)
- [Server mode](#server-mode)
- [Embed into your own project](#embed-into-your-own-project)
- [Global predef file: `~/.scala-repl-pp.sc`](#global-predef-file-scala-repl-ppsc)
- [Verbose mode](#verbose-mode)
- [Parameters cheat sheet: the most important ones](#parameters-cheat-sheet-the-most-important-ones)
- [FAQ](#faq)
  * [Is this an extension of the stock REPL or a fork?](#is-this-an-extension-of-the-stock-repl-or-a-fork)
  * [Why are script line numbers incorrect?](#why-are-script-line-numbers-incorrect)

## Benefits over / comparison with

### Regular Scala REPL
* add runtime dependencies on startup with maven coordinates - automatically handles all downstream dependencies via [coursier](https://get-coursier.io/)
* customize greeting, prompt and shutdown code
* multiple @main with named arguments (regular Scala REPL only allows an argument list)
* predef code - i.e. run custom code before starting the REPL - via string and scripts
* server mode: REPL runs embedded
* easily embeddable into your own build
* structured rendering including product labels and type information:<br/>
Scala-REPL-PP:<br/>
<img src="https://github.com/mpollmeier/scala-repl-pp/assets/506752/2e24831e-3c0d-4b07-8453-1fa267a6a6bf" width="700px"/>
<br/>
Stock Scala REPL:<br/>
<img src="https://github.com/mpollmeier/scala-repl-pp/assets/506752/77d006d1-35ef-426f-a3b8-1311a36ffed5" width="700px"/>


### [Ammonite](http://ammonite.io)
* Ammonite's Scala 3 support is far from complete - e.g. autocompletion for extension methods has [many shortcomings](https://github.com/com-lihaoyi/Ammonite/issues/1297). In comparison: scala-repl-pp uses the regular Scala3/dotty ReplDriver. 
* Ammonite has some Scala2 dependencies intermixed, leading to downstream build problems like [this](https://github.com/com-lihaoyi/Ammonite/issues/1241). It's no longer easy to embed Ammonite into your own build.
* Note: Ammonite allows to add dependencies dynamically even in the middle of the REPL session - that's not supported by scala-repl-pp yet. You need to know which dependencies you want on startup. 

### [scala-cli](https://scala-cli.virtuslab.org/)
scala-cli wraps and invokes the regular Scala REPL (or optionally Ammonite). It doesn't have a separate REPL implementation, and therefor the above differences apply. 

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

# pass some predef code in file(s)
echo 'def foo = 42' > foo.sc

./scala-repl-pp --predef foo.sc
scala> foo
val res0: Int = 42
```

### Add dependencies with maven coordinates
Note: the dependencies must be known at startup time, either via `--dep` parameter:
```
./scala-repl-pp --dep com.michaelpollmeier:versionsort:1.0.7
scala> versionsort.VersionHelper.compare("1.0", "0.9")
val res0: Int = 1
```
To add multiple dependencies, you can specify this parameter multiple times.

Alternatively, use the `//> using dep` directive in predef code or predef files:
```
echo '//> using dep com.michaelpollmeier:versionsort:1.0.7' > predef.sc

./scala-repl-pp --predef predef.sc

scala> versionsort.VersionHelper.compare("1.0", "0.9")
val res0: Int = 1
```

Note: if your dependencies are not hosted on maven central, you can [specify additional resolvers](#additional-dependency-resolvers-and-credentials) - including those that require authentication)

### Importing additional script files interactively
```
echo 'val bar = foo' > myScript.sc

./scala-repl-pp

val foo = 1
//> using file myScript.sc
println(bar) //1
```

### Rendering of output

Unlike the stock Scala REPL, scala-repl-pp does _not_ truncate the output by default. You can optionally specify the maxHeight parameter though:
```
./scala-repl-pp --maxHeight 5
scala> (1 to 100000).toSeq
val res0: scala.collection.immutable.Range.Inclusive = Range(
  1,
  2,
  3,
...
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

### Predef file(s) used in script
test-predef.sc
```scala
println(foo)
```

test-predef-file.sc
```scala
val foo = "Hello, predef file"
```

```bash
./scala-repl-pp --script test-predef.sc --predef test-predef-file.sc
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
Dependencies can be added via `//> using dep` syntax (like in scala-cli).

test-dependencies.sc:
```scala
//> using dep com.michaelpollmeier:versionsort:1.0.7

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
@main def main(first: String, last: String) = {
  println(s"Hello, $first $last!")
}
```

```bash
./scala-repl-pp --script test-main-withargs.sc --param first=Michael --param last=Pollmeier
```

## Additional dependency resolvers and credentials
Via `--repo` parameter on startup:
```bash
./scala-repl-pp --repo "https://repo.gradle.org/gradle/libs-releases" --dep org.gradle:gradle-tooling-api:7.6.1
scala> org.gradle.tooling.GradleConnector.newConnector()
```
To add multiple dependency resolvers, you can specify this parameter multiple times.

Or via `//> using resolver` directive as part of your script or predef code:

script-with-resolver.sc
```scala
//> using resolver https://repo.gradle.org/gradle/libs-releases
//> using dep org.gradle:gradle-tooling-api:7.6.1
println(org.gradle.tooling.GradleConnector.newConnector())
```
```scala
./scala-repl-pp --script script-with-resolver.sc
```

If one or multiple of your resolvers require authentication, you can configure your username/passwords in a [`credentials.properties` file](https://get-coursier.io/docs/other-credentials#property-file):
```
mycorp.realm=Artifactory Realm
mycorp.host=shiftleft.jfrog.io
mycorp.username=michael
mycorp.password=secret

otherone.username=j
otherone.password=imj
otherone.host=nexus.other.com
```
The prefix is arbitrary and is only used to specify several credentials in a single file. scala-repl-pp uses [coursier](https://get-coursier.io) to resolve dependencies. 

## Server mode
```bash
./scala-repl-pp --server

curl http://localhost:8080/query-sync -X POST -d '{"query": "val foo = 42"}'
# {"success":true,"stdout":"val foo: Int = 42\n",...}

curl http://localhost:8080/query-sync -X POST -d '{"query": "val bar = foo + 1"}'
# {"success":true,"stdout":"val bar: Int = 43\n",...}

curl http://localhost:8080/query-sync -X POST -d '{"query":"println(\"OMG remote code execution!!1!\")"}'
# {"success":true,"stdout":"",...}%
```

Predef code works with server as well:
```
echo val foo = 99 > foo.sc
./scala-repl-pp --server --predef foo.sc

curl -XPOST http://localhost:8080/query-sync -d '{"query":"val baz = foo + 1"}'
# {"success":true,"stdout":"val baz: Int = 100\n",...}
```

There's also has an asynchronous mode:
```
./scala-repl-pp --server

curl http://localhost:8080/query -X POST -d '{"query": "val baz = 93"}'
# {"success":true,"uuid":"e2640fcb-3193-4386-8e05-914b639c3184"}%

curl http://localhost:8080/result/e2640fcb-3193-4386-8e05-914b639c3184
{"success":true,"uuid":"e2640fcb-3193-4386-8e05-914b639c3184","stdout":"val baz: Int = 93\n"}%
```

And there's even a websocket channel that allows you to get notified when the query has finished. For more details and other use cases check out [ReplServerTests.scala](server/src/test/scala/replpp/server/ReplServerTests.scala)

Server-specific configuration options as per `scala-repl-pp --help`:
```
--server-host <value>    Hostname on which to expose the REPL server
--server-port <value>    Port on which to expose the REPL server
--server-auth-username <value> Basic auth username for the REPL server
--server-auth-password <value> Basic auth password for the REPL server
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

## Global predef file: `~/.scala-repl-pp.sc`
Code that should be available across all scala-repl-pp sessions can be written into your local `~/.scala-repl-pp.sc`. 

```
echo 'def bar = 90' > ~/.scala-repl-pp.sc
echo 'def baz = 91' > script1.sc
echo 'def bam = 92' > script2.sc

./scala-repl-pp --predef script1.sc --predef script2.sc

scala> bar
val res0: Int = 90

scala> baz
val res1: Int = 91

scala> bam
val res2: Int = 92
```

## Verbose mode
If verbose mode is enabled, you'll get additional information about classpaths and complete scripts etc. 
To enable it, you can either pass `--verbose` or set the environment variable `SCALA_REPL_PP_VERBOSE=true`.

## Parameters cheat sheet: the most important ones
Here's only the most important ones - run `scala-repl-pp --help` for all parameters.

| parameter     | short         | description                           
| ------------- | ------------- | --------------------------------------
| `--predef`    | `-p`          | Import additional files
| `--dep`       | `-d`          | Add dependencies via maven coordinates
| `--repo`      | `-r`          | Add repositories to resolve dependencies
| `--script`    |               | Execute given script
| `--param`     |               | key/value pair for main function in script
| `--verbose`   | `-v`          | Verbose mode

## FAQ

### Is this an extension of the stock REPL or a fork?
Technically it is a fork, i.e. we copied parts of the ReplDriver to make some adjustments. 
However, semantically, Scala-REPL-PP can be considered an extension of the stock repl. We don't want to create and maintain a competing REPL implementation, 
instead the idea is to provide a space for exploring new ideas and bringing them back into the dotty codebase. 
[When we forked](https://github.com/mpollmeier/scala-repl-pp/commit/eb2bf9a3bed681bffa945f657ada14781c6a7a14) the stock ReplDriver, we made sure to separate the commits into bitesized chunks so we can easily rebase. The changes are clearly marked, and whenever there's a new dotty version we're bringing in the relevant changes here (`git diff 3.3.0-RC5..3.3.0-RC6 compiler/src/dotty/tools/repl/`).

### Why are script line numbers incorrect?
Scala-REPL-PP currently uses a simplistic model for predef code|files and additionally imported files, and just copies everything into one large script. That simplicity naturally comes with a few limitations, e.g. line numbers may be different from the input script(s). 

A better approach would be to work with a separate compiler phase, similar to what Ammonite does. That way, we could inject all previously defined values|imports|... into the compiler, and extract all results from the compiler context. That's a goal for the future. 

If there's a compilation issue, the temporary script file will not be deleted and the error output will tell you it's path, in order to help with debugging.

