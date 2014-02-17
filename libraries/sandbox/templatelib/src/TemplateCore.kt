package kotlin.template

import kotlin.io.*

/**
 * Represents a generic API to templates which should be usable
 * in JavaScript in a browser or on the server side stand alone or in a web app etc.
 *
 * To make things easier to implement in JS this package won't refer to any java.io or servlet
 * stuff
 */
trait Template {
  var printer: Printer

  /** Renders the template to the output printer */
  fun render(): Unit
}

/**
 * Represents the output of a template which is a stream of objects
 * usually strings which then write to some underlying output stream
 * such as a java.io.Writer on the server side.
 *
 * We abstract away java.io.* APIs here to make the JS implementation simpler
 */
trait Printer {
  fun print(value: Any): Unit
  //fun print(text: String): Unit
}

class NullPrinter() : Printer {
  //override fun print(text: String) = none()
  override fun print(value: Any) {
    throw UnsupportedOperationException("No Printer defined on the Template")
  }
}

/**
 * Base class for template implementations to hold any helpful behaviour
 */
abstract class TemplateSupport : Template {
}

/**
 * Base class for templates generating text output
 * by printing values to some underlying Printer which
 * will typically output to a String, File, stream etc.
 */
abstract class TextTemplate() : TemplateSupport(), Printer {
  override var printer: Printer = NullPrinter()

  fun String.plus(): Unit {
    printer.print(this)
  }

  //override fun print(value: String) = printer.print(value)

  override fun print(value: Any) = printer.print(value)
}
