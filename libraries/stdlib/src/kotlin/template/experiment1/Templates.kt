package kotlin.template.experiment1

import org.w3c.dom.Node
import kotlin.dom.toXmlString

fun StringBuilder.text(value : String) : Unit {
    this.append(value)
}

fun StringBuilder.expression(value : Any?) : Unit {
    this.append(value)
}

/**
 * Converts a [[StringTemplate]] to a String
 */
// TODO tried toString() but compiler error kicks in
fun format(fn : (StringBuilder) -> Unit) : String {
    val builder = StringBuilder()
    fn(builder)
    return builder.toString() ?: ""
}


abstract class TextBuilder<T>(val output: Appendable) {

/*
    */
/**
     * Converts the given string template to a String

    fun toString(fn: (T) -> Unit): String = toString(ToStringFormatter, fn)

    fun toString(formatter : ValueFormatter, fn: (T) -> Unit): String {
        fn(this)
        return output.toString() ?: ""
    }
*/

    fun text(value: String): Unit {
        output.append(value)
    }

}

class HtmlBuilder(output: Appendable) : TextBuilder<HtmlBuilder>(output) {
    var nullText : String = ""

    fun expression(value: Node): Unit {
        text(value.toXmlString())
    }

    fun expression(value: Any?): Unit {
        if (value == null) {
            output.append(nullText)
        } else {
            escape(value.toString() ?: "")
        }
    }

    fun escape(text : String) : Unit {
        for (c in text) {
            if (c == '<') output.append("&lt;")
            else if (c == '>') output.append("&gt;")
            else if (c == '&') output.append("&amp;")
            else if (c == '"') output.append("&quot;")
            else output.append(c)
        }
    }
}


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

fun toHtml(fn: (HtmlBuilder) -> Unit): String {
    val buffer = StringBuilder()
    val html = HtmlBuilder(buffer)
    fn(html)
    return buffer.toString() ?: ""
}
