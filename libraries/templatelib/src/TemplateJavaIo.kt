// Server side Java IO code to avoid coupling
// the core template code to java.* for easier JS porting
package kotlin.template.io

import kotlin.template.*
import java.io.Writer
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.io.StringWriter

/**
 * A Printer implementation which uses a Writer
 */
class WriterPrinter(val writer: Writer) : Printer {

  override fun print(value: Any) {
    // TODO should be using a formatter to do the conversion
    writer.write(value.toString())
  }
}

fun Template.renderToText(): String {
  val buffer = StringWriter()
  renderTo(buffer)
  return buffer.toString().sure()
}

fun Template.renderTo(writer: Writer): Unit {
  this.printer = WriterPrinter(writer)
  this.render()
}

fun Template.renderTo(os: OutputStream): Unit = renderTo(OutputStreamWriter(os))
