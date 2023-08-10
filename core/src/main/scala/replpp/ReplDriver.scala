package replpp

import dotty.tools.MainGenericCompiler.classpathSeparator
import dotty.tools.dotc.Run
import dotty.tools.dotc.ast.{Positioned, tpd, untpd}
import dotty.tools.dotc.classpath.{AggregateClassPath, ClassPathFactory}
import dotty.tools.dotc.config.{Feature, JavaPlatform, Platform}
import dotty.tools.dotc.core.Comments.{ContextDoc, ContextDocstrings}
import dotty.tools.dotc.core.Contexts.{Context, ContextBase, ContextState, FreshContext, ctx, explore}
import dotty.tools.dotc.core.{Contexts, MacroClassLoader, Mode, TyperState}
import dotty.tools.io.{AbstractFile, ClassPath, ClassRepresentation}
import dotty.tools.repl.*
import org.jline.reader.*

import java.io.PrintStream
import java.lang.System.lineSeparator
import java.net.URL
import java.nio.file.Path
import javax.naming.InitialContext
import scala.annotation.tailrec
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors


class ReplDriver(args: Array[String],
                 out: PrintStream = scala.Console.out,
                 onExitCode: Option[String] = None,
                 greeting: Option[String],
                 prompt: String,
                 maxHeight: Option[Int] = None,
                 nocolors: Boolean = false,
                 classLoader: Option[ClassLoader] = None) extends ReplDriverBase(args, out, maxHeight, nocolors, classLoader) {

//  private val executorService = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  private val init = {
//    System.setSecurityManager(new SecurityManager() {
//      override def checkExit(status: Int): Unit = {
//        new Exception("exit attempt with return code " + status).printStackTrace()
//      } // note that all dedicated check... methods delegate to the two below,
//
//      // so overriding these is sufficient to enable all other actions
//      def checkPermission(perm: Nothing, context: AnyRef): Unit = {
//      }
//
//      def checkPermission(perm: Nothing): Unit = {
//      }
//    })


//    sun.misc.SharedSecrets.getJavaLangAccess.registerShutdownHook(7, true, () => {
//      println("AA0")
//      println(Thread.currentThread)
//      new Exception("Who called me?").printStackTrace()
//
//    })

//    println("BB0")
    val printingHook = new Thread(() => println("In the middle of a shutdown"))
    Runtime.getRuntime.addShutdownHook(printingHook)
//    java.lang.ApplicationShutdownHooks
//    println("BB1: installed")
    ()
  }

  //  var executorThread: Option[Thread] = None


  /** Run REPL with `state` until `:quit` command found
    * Main difference to the 'original': different greeting, trap Ctrl-c
   */
  override def runUntilQuit(using initialState: State = initialState)(): State = {
    val terminal = new replpp.JLineTerminal {
      override protected def promptStr = prompt
    }
    greeting.foreach(out.println)

    @tailrec
    def loop(using state: State)(): State = {
      Try {
        // TODO check: readLine _does_ trap C-c, doesn't it?
        println("AA0 readLine:start")
        val inputLines = readLine(terminal, state)
        println("AA1 readLine:end")
//        var newState: State = state
//        var finished = false

//        val t = new Thread(new Runnable {
//          def run = {
//            println("in thread: start")
//            val result = Try(interpretInput(inputLines, state, pwd))
//            println(s"in thread: result.isSuccess=${result.isSuccess}")
//            newState = result.get
//            finished = true
//          }
//        })
////        t.run()
//        t.start()

        // TODO use some semaphore, or executorservice singlethreaded (with future), or t.join, or...
//        while (!finished) {
//          println(s"waiting for result; t.state=${t.getState}")
//          Thread.sleep(1000)
//        }

        // t.shutdown?
//        println(s"XX1")
//        newState

//      try {
//        interpretInput(inputLines, state, pwd)
//      } catch {
//        // doesn't get caught
//        case t: java.io.IOError =>
//          println("FF0")
//          t.printStackTrace()
//          throw t
//        case t =>
//          println("FF1")
//          t.printStackTrace()
//          throw t
//      }

        interpretInput(inputLines, state, pwd)
      } match {
        case Success(newState) =>
          loop(using newState)()
        case Failure(_: EndOfFileException) =>
          // Ctrl+D -> user wants to quit
//          println("XX8")
          onExitCode.foreach(code => run(code)(using state))
          state
        case Failure(_: UserInterruptException) =>
          // Ctrl+C -> swallow, do nothing
//          println("XX9")
          loop(using state)()
        case Failure(exception) =>
          throw exception
      }
    }

    try runBody {
//      var newState: State = null
//      var finished = false
//      println("YY0")
//      executorService.submit {
//        new Runnable {
//          override def run(): Unit = {
//            println("YY1")
//            newState = loop(using initialState)()
//            finished = true
//            println("YY2")
//          }
//        }
//      }
//      println("YY3")
//      newState
      loop(using initialState)()
    }
    finally terminal.close()
  }

  /** Blockingly read a line, getting back a parse result.
    * The input may be multi-line.
    * If the input contains a using file directive (e.g. `//> using file abc.sc`), then we interpret everything up
    * until the directive, then interpret the directive (i.e. import that file) and continue with the remainder of
    * our input. That way, we import the file in-place, while preserving line numbers for user feedback.  */
  private def readLine(terminal: replpp.JLineTerminal, state: State): IterableOnce[String] = {
    given Context = state.context
    val completer: Completer = { (_, line, candidates) =>
      val comps = completions(line.cursor, line.line, state)
      candidates.addAll(comps.asJava)
    }
    terminal.readLine(completer).linesIterator
  }

}
