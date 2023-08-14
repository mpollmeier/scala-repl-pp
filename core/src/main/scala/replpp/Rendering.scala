package replpp

import scala.language.unsafeNulls

import dotty.tools.dotc.*
import dotty.tools.dotc.core.*
import dotty.tools.repl.AbstractFileClassLoader
import Contexts.*, Denotations.*, Flags.*, NameOps.*, StdNames.*, Symbols.*
import printing.ReplPrinter
import reporting.Diagnostic
import transform.ValueClasses
import util.StackTraceOps.*

import scala.util.control.NonFatal

/** Based on https://github.com/lampepfl/dotty/blob/3.3.0-RC5/compiler/src/dotty/tools/repl/Rendering.scala
 *
 * This rendering object uses `ClassLoader`s to accomplish crossing the 4th
 *  wall (i.e. fetching back values from the compiled class files put into a
 *  specific class loader capable of loading from memory) and rendering them.
 *
 *  @pre this object should be paired with a compiler session, i.e. when
 *       `ReplDriver#resetToInitial` is called, the accompanying instance of
 *       `Rendering` is no longer valid.
 */
private[replpp] class Rendering(maxHeight: Option[Int], parentClassLoader: Option[ClassLoader] = None)(using colors: Colors):

  import Rendering._

  var myClassLoader: AbstractFileClassLoader = _

  /** (value, maxElements, maxCharacters) => String */
  var myReplStringOf: (Object, Int, Int) => String = _

  /** Class loader used to load compiled code */
  private[replpp] def classLoader()(using Context) =
    if (myClassLoader != null && myClassLoader.root == ctx.settings.outputDir.value) myClassLoader
    else {
      val parent = Option(myClassLoader).orElse(parentClassLoader).getOrElse {
        val compilerClasspath = ctx.platform.classPath(using ctx).asURLs
        // We can't use the system classloader as a parent because it would
        // pollute the user classpath with everything passed to the JVM
        // `-classpath`. We can't use `null` as a parent either because on Java
        // 9+ that's the bootstrap classloader which doesn't contain modules
        // like `java.sql`, so we use the parent of the system classloader,
        // which should correspond to the platform classloader on Java 9+.
        val baseClassLoader = ClassLoader.getSystemClassLoader.getParent
        new java.net.URLClassLoader(compilerClasspath.toArray, baseClassLoader)
      }

      myClassLoader = new AbstractFileClassLoader(ctx.settings.outputDir.value, parent)
      myReplStringOf = {
        /**
          * The stock Scala REPL's rendering is suboptimal:
          * - it doesn't format the output for better readability
          * - the color highlighting is based on the string representation, i.e. a lot of information
          *   about the value it wants to render is lost, such as product labels, type information etc. 
          * Therefor this part was rewritten for replpp. 
          * 
          * Just like in the regular REPL (see dotty.tools.repl.Rendering), we need to use the PPrinter class
          * from the on the user classpath, and not the one available in the current classloader, so we
          * use reflection instead of simply calling `replpp.PPrinter:apply`.
          **/
        val pprinter = Class.forName("replpp.PPrinter", true, myClassLoader)
        val renderingMethod = pprinter.getMethod("apply", classOf[Object], classOf[Int], classOf[Boolean])
        val nocolors = colors match {
          case Colors.BlackWhite => true
          case Colors.Default => false
        }

        (objectToRender: Object, maxElements: Int, maxCharacters: Int) => {
          renderingMethod.invoke(null, objectToRender, maxHeight.getOrElse(Int.MaxValue), nocolors).asInstanceOf[String]
        }
      }
      myClassLoader
    }

  /** Return a String representation of a value we got from `classLoader()`. */
  private[replpp] def replStringOf(value: Object)(using Context): String =
    assert(myReplStringOf != null,
      "replStringOf should only be called on values creating using `classLoader()`, but `classLoader()` has not been called so far")
    val maxPrintElements = ctx.settings.VreplMaxPrintElements.valueIn(ctx.settingsState)
    val maxPrintCharacters = ctx.settings.VreplMaxPrintCharacters.valueIn(ctx.settingsState)
    val res = myReplStringOf(value, maxPrintElements, maxPrintCharacters)
    if res == null then "null // non-null reference has null-valued toString" else res

  /** Load the value of the symbol using reflection.
   *
   *  Calling this method evaluates the expression using reflection
   */
  private def valueOf(sym: Symbol)(using Context): Option[String] =
    val objectName = sym.owner.fullName.encode.toString.stripSuffix("$")
    val resObj: Class[?] = Class.forName(objectName, true, classLoader())
    val symValue = resObj
      .getDeclaredMethods
      .find(_.getName == sym.name.encode.toString)
      .flatMap { method =>
        val invocationResult = method.invoke(null)
        rewrapValueClass(sym.info.classSymbol, invocationResult)
      }
    val valueString = symValue.map(replStringOf)

    if (!sym.is(Flags.Method) && sym.info == defn.UnitType)
      None
    else
      valueString.map { s =>
        if (s.startsWith(REPL_WRAPPER_NAME_PREFIX))
          s.drop(REPL_WRAPPER_NAME_PREFIX.length).dropWhile(c => c.isDigit || c == '$')
        else
          s
      }

  /** Rewrap value class to their Wrapper class
   *
   * @param sym Value Class symbol
   * @param value underlying value
   */
  private def rewrapValueClass(sym: Symbol, value: Object)(using Context): Option[Object] =
    if ValueClasses.isDerivedValueClass(sym) then
      val valueClass = Class.forName(sym.binaryClassName, true, classLoader())
      valueClass.getConstructors.headOption.map(_.newInstance(value))
    else
      Some(value)

  def renderTypeDef(d: Denotation)(using Context): Diagnostic =
    infoDiagnostic("// defined " ++ d.symbol.showUser, d)

  def renderTypeAlias(d: Denotation)(using Context): Diagnostic =
    infoDiagnostic("// defined alias " ++ d.symbol.showUser, d)

  /** Render method definition result */
  def renderMethod(d: Denotation)(using Context): Diagnostic =
    infoDiagnostic(d.symbol.showUser, d)

  /** Render value definition result */
  def renderVal(d: Denotation)(using Context): Either[ReflectiveOperationException, Option[Diagnostic]] =
    val dcl = SyntaxHighlighting.highlight(d.symbol.showUser)
    def msg(s: String) = infoDiagnostic(s, d)
    try
      Right(
        if d.symbol.is(Flags.Lazy) then Some(msg(dcl))
        else valueOf(d.symbol).map(value => msg(s"$dcl = $value"))
      )
    catch case e: ReflectiveOperationException => Left(e)
  end renderVal

  /** Force module initialization in the absence of members. */
  def forceModule(sym: Symbol)(using Context): Seq[Diagnostic] =
    def load() =
      val objectName = sym.fullName.encode.toString
      Class.forName(objectName, true, classLoader())
      Nil
    try load()
    catch
      case e: ExceptionInInitializerError => List(renderError(e, sym.denot))
      case NonFatal(e) => List(renderError(e, sym.denot))

  /** Render the stack trace of the underlying exception. */
  def renderError(thr: Throwable, d: Denotation)(using Context): Diagnostic =
    val cause = rootCause(thr)
    // detect
    //at repl$.rs$line$2$.<clinit>(rs$line$2:1)
    //at repl$.rs$line$2.res1(rs$line$2)
    def isWrapperInitialization(ste: StackTraceElement) =
      ste.getClassName.startsWith(REPL_WRAPPER_NAME_PREFIX)  // d.symbol.owner.name.show is simple name
      && (ste.getMethodName == nme.STATIC_CONSTRUCTOR.show || ste.getMethodName == nme.CONSTRUCTOR.show)

    infoDiagnostic(cause.formatStackTracePrefix(!isWrapperInitialization(_)), d)
  end renderError

  private def infoDiagnostic(msg: String, d: Denotation)(using Context): Diagnostic =
    new Diagnostic.Info(msg, d.symbol.sourcePos)

object Rendering:
  final val REPL_WRAPPER_NAME_PREFIX = str.REPL_SESSION_LINE

  extension (s: Symbol)
    def showUser(using Context): String = {
      val printer = new ReplPrinter(ctx)
      val text = printer.dclText(s)
      text.mkString(ctx.settings.pageWidth.value, ctx.settings.printLines.value)
    }

  def rootCause(x: Throwable): Throwable = x match
    case _: ExceptionInInitializerError |
         _: java.lang.reflect.InvocationTargetException |
         _: java.lang.reflect.UndeclaredThrowableException |
         _: java.util.concurrent.ExecutionException
        if x.getCause != null =>
      rootCause(x.getCause)
    case _ => x
