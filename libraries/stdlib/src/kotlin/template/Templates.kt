package kotlin.template

import kotlin.dom.*
import org.w3c.dom.Node
import com.sun.org.apache.xalan.internal.xsltc.dom.UnionIterator

// TODO this class should move into the runtime
// in jet.StringTemplate
class StringTemplate(val values: Array<Any?>) {

    fun toString(): String {
        val out = StringBuilder()
        forEach{ out.append(it) }
        return out.toString() ?: ""
    }

    /**
     * Performs the given function on each value in the collection
     */
    fun forEach(fn: (Any?) -> Unit): Unit {
        for (v in values) {
            fn(v)
        }
    }
}

fun StringTemplate.toString(formatter : ValueFormatter): String {
    val buffer = StringBuilder()
    append(buffer, formatter)
    return buffer.toString() ?: ""
}

fun StringTemplate.append(out: Appendable, formatter: ValueFormatter): Unit {
    var constantText = true
    this.forEach {
        if (constantText) {
            if (it == null) {
                throw IllegalStateException("No constant checks should be null");
            } else {
                val text = it.toString()
                if (text != null) {
                    out.append(text)
                }
            }
        } else {
            formatter.format(out, it)
        }
        constantText = !constantText
    }
}


fun StringTemplate.toHtml(formatter : HtmlFormatter = HtmlFormatter()): String = toString(formatter)

/**
 * Represents a formatter of values in a [[StringTemplate]] which understands how to escape
 * different types for different kinds of language
 */
trait ValueFormatter {
    fun format(buffer : Appendable, val value : Any?) : Unit
}

object ToStringFormatter : ValueFormatter {

    fun toString() = "ToStringFormatter"

    override fun format(buffer : Appendable, value : Any?) {
        val text = if (value == null) "null" else value.toString()
        buffer.append(text)
    }
}

class HtmlFormatter : ValueFormatter {
    var nullText : String = ""

    override fun format(buffer : Appendable, value : Any?) {
        if (value == null) {
            buffer.append(nullText)
        } else if (value is StringTemplate) {
            value.append(buffer, this)
        } else if (value is Node) {
            buffer.append(value.toXmlString())
        } else {
            escape(buffer, value.toString() ?: "")
        }
    }

    fun escape(buffer : Appendable, text : String) : Unit {
        for (c in text) {
            if (c == '<') buffer.append("&lt;")
            else if (c == '>') buffer.append("&gt;")
            else if (c == '&') buffer.append("&amp;")
            else if (c == '"') buffer.append("&quot;")
            else buffer.append(c)
        }
    }
}
