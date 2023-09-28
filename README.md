[![Release](https://github.com/mpollmeier/scala-repl-pp/actions/workflows/release.yml/badge.svg)](https://github.com/mpollmeier/scala-repl-pp/actions/workflows/release.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.michaelpollmeier/scala-repl-pp_3/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.michaelpollmeier/scala-repl-pp_3)

## srp: scala-repl-pp (or even longer: Scala REPL PlusPlus)
srp wraps the stock Scala 3 REPL and adds many features inspired by ammonite and scala-cli. srp has only one (direct) dependency: the scala3-compiler[(*)](#fineprint). 

This is (also) a breeding ground for improvements to the stock Scala REPL: we're forking parts of the REPL to later bring the changes back into the dotty codebase.

## Installation / quick start
```bash
# download latest release and make executable
curl -fL https://github.com/mpollmeier/scala-repl-pp/releases/latest/download/srp > srp
chmod +x srp

# move whereever you want to have it - the directory should be on your PATH, e.g.
sudo mv srp /usr/local/bin/srp

srp
```
Prerequisite: jdk11+

## TOC
<!-- markdown-toc --maxdepth 3 README.md|tail -n +4 -->
- [Benefits over / comparison with](#benefits-over--comparison-with)
  * [Regular Scala REPL](#regular-scala-repl)
  * [Ammonite](#ammonite)
  * [scala-cli](#scala-cli)
- [REPL](#repl)
  * [Operators: Redirect to file, pipe to external command](#operators-redirect-to-file-pipe-to-external-command)
  * [Add dependencies with maven coordinates](#add-dependencies-with-maven-coordinates)
  * [Importing additional script files interactively](#importing-additional-script-files-interactively)
  * [Rendering of output](#rendering-of-output)
  * [Exiting the REPL](#exiting-the-repl)
- [Scripting](#scripting)
  * [Simple "Hello world" script](#simple-hello-world-script)
  * [Predef file(s) used in script](#predef-files-used-in-script)
  * [Importing files / scripts](#importing-files--scripts)
  * [Dependencies](#dependencies)
  * [@main entrypoints](#main-entrypoints)
  * [multiple @main entrypoints: test-main-multiple.sc](#multiple-main-entrypoints-test-main-multiplesc)
  * [named parameters](#named-parameters)
- [Additional dependency resolvers and credentials](#additional-dependency-resolvers-and-credentials)
  * [Attach a debugger (remote jvm debug)](#attach-a-debugger-remote-jvm-debug)
- [Server mode](#server-mode)
- [Embed into your own project](#embed-into-your-own-project)
- [Global predef file: `~/.srp.sc`](#global-predef-file-srpsc)
- [Verbose mode](#verbose-mode)
- [Parameters cheat sheet: the most important ones](#parameters-cheat-sheet-the-most-important-ones)
- [FAQ](#faq)
  * [Is this an extension of the stock REPL or a fork?](#is-this-an-extension-of-the-stock-repl-or-a-fork)
  * [Why are script line numbers incorrect?](#why-are-script-line-numbers-incorrect)
  * [Why do we ship a shaded copy of other libraries and not use dependencies?](#why-do-we-ship-a-shaded-copy-of-other-libraries-and-not-use-dependencies)
  * [Where's the cache located on disk?](#wheres-the-cache-located-on-disk)
- [Contribution guidelines](#contribution-guidelines)
  * [How can I build/stage a local version?](#how-can-i-buildstage-a-local-version)
  * [How can I get a new binary (bootstrapped) release?](#how-can-i-get-a-new-binary-bootstrapped-release)
  * [Updating the Scala version](#updating-the-scala-version)
  * [Updating the shaded libraries](#updating-the-shaded-libraries)
- [Fineprint](#fineprint)
  
  
  
  
## Benefits over / comparison with

### Regular Scala REPL
* add runtime dependencies on startup with maven coordinates - automatically handles all downstream dependencies via [coursier](https://get-coursier.io/)
* `#>`, `#>>` and `#|` operators to redirect output to file and pipe to external command
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
* Ammonite's Scala 3 support is far from complete - e.g. autocompletion for extension methods has [many shortcomings](https://github.com/com-lihaoyi/Ammonite/issues/1297). In comparison: srp uses the regular Scala3/dotty ReplDriver. 
* Ammonite has some Scala2 dependencies intermixed, leading to downstream build problems like [this](https://github.com/com-lihaoyi/Ammonite/issues/1241). It's no longer easy to embed Ammonite into your own build.
* Note: Ammonite allows to add dependencies dynamically even in the middle of the REPL session - that's not supported by srp currently. You need to know which dependencies you want on startup. 

### [scala-cli](https://scala-cli.virtuslab.org/)
* srp has a 66.6% shorter name :slightly_smiling_face:
scala-cli wraps and invokes the regular Scala REPL (by default; or optionally Ammonite). It doesn't modify/fix the REPL itself, i.e. the above mentioned differences between srp and the stock scala repl (or alternatively Ammonite) apply, with the exception of dependencies: scala-cli does let you add them on startup as well.

## REPL

```bash
# run with defaults
srp

# customize prompt, greeting and exit code
srp --prompt myprompt --greeting 'hey there!' --onExitCode 'println("see ya!")'

# pass some predef code in file(s)
echo 'def foo = 42' > foo.sc

srp --predef foo.sc
scala> foo
val res0: Int = 42
```

### Operators: Redirect to file, pipe to external command
Inspired by unix shell redirection and pipe operators (`>`, `>>` and `|`) you can redirect output into files with `#>` (overrides existing file) and `#>>` (create or append to file), and use `#|` to pipe the output to a command, such as `less`:
```scala
srp

scala> "hey there" #>  "out.txt"
scala> "hey again" #>> "out.txt"
scala> Seq("a", "b", "c") #>> "out.txt"

// pipe results to external command
scala> Seq("a", "b", "c") #| "cat"
val res0: String = """a
b
c"""

// pipe results to external command with arguments
scala> Seq("foo", "bar", "foobar") #| ("grep", "foo")
val res1: String = """foo
foobar"""

// pipe results to external command and let it inherit stdin/stdout
scala> Seq("a", "b", "c") #|^ "less"

// pipe results to external command with arguments and let it inherit stdin/stdout
scala> Seq("a", "b", "c") #|^ ("less", "-N")
```

All operators use the same pretty-printing that's used within the REPL, i.e. you get structured rendering including product labels etc. 
```scala
scala> case class PrettyPrintable(s: String, i: Int)
scala> PrettyPrintable("two", 2) #> "out.txt"
// out.txt now contains `PrettyPrintable(s = "two", i = 2)`
```

The operators have a special handling for two common use cases that are applied at the root level of the object you hand them: list- or iterator-type objects are unwrapped and their elements are rendered in separate lines, and Strings are rendered without the surrounding `""`. Examples:
```scala
scala> "a string" #> "out.txt"
// rendered as `a string` without quotes

scala> Seq("one", "two") #> "out.txt"
// rendered as two lines without quotes:
// one
// two

scala> Seq("one", Seq("two"), Seq("three", 4), 5) #> "out.txt"
// top-level list-types are unwrapped
// resulting top-level strings are rendered without quotes:
// one
// List("two")
// List("three", 4)
// 5
```

All operators are prefixed with `#` in order to avoid naming clashes with more basic operators like `>` for greater-than-comparisons. This naming convention is inspired by scala.sys.process.

### Add dependencies with maven coordinates
Note: the dependencies must be known at startup time, either via `--dep` parameter:
```
srp --dep com.michaelpollmeier:versionsort:1.0.7
scala> versionsort.VersionHelper.compare("1.0", "0.9")
val res0: Int = 1
```
To add multiple dependencies, you can specify this parameter multiple times.

Alternatively, use the `//> using dep` directive in predef code or predef files:
```
echo '//> using dep com.michaelpollmeier:versionsort:1.0.7' > predef.sc

srp --predef predef.sc

scala> versionsort.VersionHelper.compare("1.0", "0.9")
val res0: Int = 1
```

For Scala dependencies use `::`:
```
srp --dep com.michaelpollmeier::colordiff:0.36
colordiff.ColorDiff(List("a", "b"), List("a", "bb"))
// color coded diff
```

Note: if your dependencies are not hosted on maven central, you can [specify additional resolvers](#additional-dependency-resolvers-and-credentials) - including those that require authentication)

Implementation note: srp uses [coursier](https://get-coursier.io/) to fetch the dependencies. We invoke it in a subprocess via the coursier java launcher, in order to give our users maximum control over the classpath.

### Importing additional script files interactively
```
echo 'val bar = foo' > myScript.sc

srp

val foo = 1
//> using file myScript.sc
println(bar) //1
```

You can specify the filename with relative or absolute paths:
```java
//> using file scripts/myScript.sc
//> using file ../myScript.sc
//> using file /path/to/myScript.sc
```


### Rendering of output

Unlike the stock Scala REPL, srp does _not_ truncate the output by default. You can optionally specify the maxHeight parameter though:
```
srp --maxHeight 5
scala> (1 to 100000).toSeq
val res0: scala.collection.immutable.Range.Inclusive = Range(
  1,
  2,
  3,
...
```

### Exiting the REPL
Famously one of the most popular question on stackoverflow is about how to exit `vim` - fortunately you can apply the answer as-is to exit srp :slightly_smiling_face:
```
// all of the following exit the REPL
:exit
:quit
:q
```

When the REPL is waiting for input we capture `Ctrl-c` and don't exit. If there's currently a long-running execution that you really *might* want to cancel you can press `Ctrl-c` again immediately which will kill the entire repl:
```
scala> Thread.sleep(50000)
// press Ctrl-c
Captured interrupt signal `INT` - if you want to kill the REPL, press Ctrl-c again within three seconds

// press Ctrl-c again will exit the repl
$
```
Context: we'd prefer to cancel the long-running operation, but that's not so easy on the JVM.

## Scripting

See [ScriptRunnerTest](core/src/test/scala/replpp/scripting/ScriptRunnerTest.scala) for a more complete and in-depth overview.

### Simple "Hello world" script
test-simple.sc
```scala
println("Hello!")
"i was here" #> "out.txt"
```

```bash
srp --script test-simple.sc
cat out.txt # prints 'i was here'
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
srp --script test-predef.sc --predef test-predef-file.sc
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
srp --script test.sc
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
srp --script test-dependencies.sc
```

Note: this also works with `using` directives in your predef code - for script and REPL mode.

### @main entrypoints
test-main.sc
```scala
@main def main() = println("Hello, world!")
```

```bash
srp --script test-main.sc
```

### multiple @main entrypoints: test-main-multiple.sc
```scala
@main def foo() = println("foo!")
@main def bar() = println("bar!")
```

```bash
srp --script test-main-multiple.sc --command foo
```

### named parameters
test-main-withargs.sc
```scala
@main def main(first: String, last: String) = {
  println(s"Hello, $first $last!")
}
```

```bash
srp --script test-main-withargs.sc --param first=Michael --param last=Pollmeier
```
Note that on windows the parameters need to be triple-quoted:
`srp.bat --script test-main-withargs.sc --param """first=Michael""" --param """last=Pollmeier"""`

## Additional dependency resolvers and credentials
Via `--repo` parameter on startup:
```bash
srp --repo "https://repo.gradle.org/gradle/libs-releases" --dep org.gradle:gradle-tooling-api:7.6.1
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
srp --script script-with-resolver.sc
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
The prefix is arbitrary and is only used to specify several credentials in a single file. srp uses [coursier](https://get-coursier.io) to resolve dependencies. 

### Attach a debugger (remote jvm debug)
```
srp --script myScript.sc --remoteJvmDebug
```
Then attach your favorite IDE / debugger on port 5005. 

## Server mode
Note: srp-server isn't currently available as a bootstrapped binary, so you have to [stage it locally](#how-can-i-buildstage-a-local-version) first using `sbt stage`.
```bash
./srp-server

curl http://localhost:8080/query-sync -X POST -d '{"query": "val foo = 42"}'
# {"success":true,"stdout":"val foo: Int = 42\n",...}

curl http://localhost:8080/query-sync -X POST -d '{"query": "val bar = foo + 1"}'
# {"success":true,"stdout":"val bar: Int = 43\n",...}

curl http://localhost:8080/query-sync -X POST -d '{"query":"println(\"OMG remote code execution!!1!\")"}'
# {"success":true,"stdout":"",...}%
```

The same for windows and powershell:
```
srp-server.bat

Invoke-WebRequest -Method 'Post' -Uri http://localhost:8080/query-sync -ContentType "application/json" -Body '{"query": "val foo = 42"}'
# Content           : {"success":true,"stdout":"val foo: Int = 42\r\n","uuid":"02f843ba-671d-4fb5-b345-91c1dcf5786d"}
Invoke-WebRequest -Method 'Post' -Uri http://localhost:8080/query-sync -ContentType "application/json" -Body '{"query": "foo + 1"}'
# Content           : {"success":true,"stdout":"val res0: Int = 43\r\n","uuid":"dc49df42-a390-4177-98d0-ac87a277c7d5"}
```

Predef code works with server as well:
```
echo val foo = 99 > foo.sc
./srp-server --predef foo.sc

curl -XPOST http://localhost:8080/query-sync -d '{"query":"val baz = foo + 1"}'
# {"success":true,"stdout":"val baz: Int = 100\n",...}
```

There's also has an asynchronous mode:
```
./srp-server

curl http://localhost:8080/query -X POST -d '{"query": "val baz = 93"}'
# {"success":true,"uuid":"e2640fcb-3193-4386-8e05-914b639c3184"}%

curl http://localhost:8080/result/e2640fcb-3193-4386-8e05-914b639c3184
{"success":true,"uuid":"e2640fcb-3193-4386-8e05-914b639c3184","stdout":"val baz: Int = 93\n"}%
```

And there's even a websocket channel that allows you to get notified when the query has finished. For more details and other use cases check out [ReplServerTests.scala](server/src/test/scala/replpp/server/ReplServerTests.scala)

Server-specific configuration options as per `srp --help`:
```
--server-host <value>    Hostname on which to expose the REPL server
--server-port <value>    Port on which to expose the REPL server
--server-auth-username <value> Basic auth username for the REPL server
--server-auth-password <value> Basic auth password for the REPL server
```

## Embed into your own project
Try out the working [string calculator example](src/test/resources/demo-project) in this repo:
```bash
cd core/src/test/resources/demo-project
sbt stage
./stringcalc

Welcome to the magical world of string calculation!
Type `help` for help

stringcalc> add(One, Two)
val res0: stringcalc.Number = Number(3)
```

## Global predef file: `~/.srp.sc`
Code that should be available across all srp sessions can be written into your local `~/.srp.sc`. 

```
echo 'def bar = 90' > ~/.srp.sc
echo 'def baz = 91' > script1.sc
echo 'def bam = 92' > script2.sc

./srp --predef script1.sc --predef script2.sc

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
Here's only the most important ones - run `srp --help` for all parameters.

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
However, semantically, srp can be considered an extension of the stock repl. We don't want to create and maintain a competing REPL implementation, 
instead the idea is to provide a space for exploring new ideas and bringing them back into the dotty codebase. 
[When we forked](https://github.com/mpollmeier/scala-repl-pp/commit/eb2bf9a3bed681bffa945f657ada14781c6a7a14) the stock ReplDriver, we made sure to separate the commits into bitesized chunks so we can easily rebase. The changes are clearly marked, and whenever there's a new dotty version we're bringing in the relevant changes here (`git diff 3.3.0-RC5..3.3.0-RC6 compiler/src/dotty/tools/repl/`).

### Why are script line numbers incorrect?
srp currently uses a simplistic model for predef code|files and additionally imported files, and just copies everything into one large script. That simplicity naturally comes with a few limitations, e.g. line numbers may be different from the input script(s). 

A better approach would be to work with a separate compiler phase, similar to what Ammonite does. That way, we could inject all previously defined values|imports|... into the compiler, and extract all results from the compiler context. That's a goal for the future. 

If there's a compilation issue, the temporary script file will not be deleted and the error output will tell you it's path, in order to help with debugging.

### Why do we ship a shaded copy of other libraries and not use dependencies?
srp includes some small libraries (e.g. most of the com-haoyili universe) that have been copied as-is, but then moved into the `replpp.shaded` namespace. We didn't include them as regular dependencies, because repl users may want to use a different version of them, which may be incompatible with the version the repl uses. Thankfully their license is very permissive - a big thanks to the original authors! The instructions of how to (re-) import then and which versions were used are available in [import-instructions.md](shaded-libs/import-instructions.md).

### Where's the cache located on disk?
The cache? The caches you mean! :)
There's `~/.cache/scala-repl-pp` for the repl itself. Since we use coursier (via a subprocess) there's also `~/.cache/coursier`. 


## Contribution guidelines

### How can I build/stage a local version?
```bash
sbt stage
./srp
```

### How can I get a new binary (bootstrapped) release?
While maven central jar releases are created for each commit on master (a new version tag is assigned automatically), the binary (bootstrapped) releases that end up in https://github.com/mpollmeier/scala-repl-pp/releases/latest are being triggered manually. Contributors can run the [bootstrap action](https://github.com/mpollmeier/scala-repl-pp/actions/workflows/bootstrap.yml).

### Updating the Scala version
* bump version in [build.sbt](build.sbt)
* get relevant diff from dotty repo
```bash
cd /path/to/dotty
git fetch

OLD=3.3.0 # set to version that was used before you bumped it
NEW=3.3.1 # set to version that you bumped it to
git checkout $NEW
git diff $OLD compiler/src/dotty/tools/repl
```
* check if any of those changes need to be reapplied to this repo

### Updating the shaded libraries
See [import-instructions.md](shaded-libs/import-instructions.md).


## Fineprint
(*) To keep our codebase concise we do use libraries, most importantly the [com.lihaoyi](https://github.com/com-lihaoyi/) stack. We want to ensure that users can freely use their own dependencies without clashing with the srp classpath though, so we [copied them into our build](shaded-libs/src/main/scala/replpp/shaded) and [changed the namespace](shaded-libs/import-instructions) to `replpp.shaded`. Many thanks to the original authors, also for choosing permissive licenses. 
  
  
