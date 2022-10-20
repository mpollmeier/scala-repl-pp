## scala-repl-pp
Scala 3 Repl PlusPlus ;) 
A (slightly) better Scala3 / dotty repl.

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

