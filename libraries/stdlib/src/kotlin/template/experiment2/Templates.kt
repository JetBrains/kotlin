package kotlin.template.experiment2

import kotlin.dom.*
import org.w3c.dom.Node


/**
 * Creates a string template from some constant string expressions and some dynamic expressions.
 */
class StringTemplate(val constantText : Array<String>, val expressions : Array<Any?>) {

    /**
     * Converts the given string template to a String
     */
    fun toString(): String = toString(ToStringFormatter)

    fun toString(formatter : ValueFormatter): String {
        val buffer = StringBuilder()
        append(buffer, formatter)
        return buffer.toString() ?: ""
    }

    fun append(buffer: Appendable, formatter: ValueFormatter): Unit {
        val expressionSize = expressions.size
        for (i in 0.upto(constantText.size - 1)) {
            buffer.append(constantText[i])
            if (i < expressionSize) {
                val value = expressions[i]
                formatter.format(buffer, value)
            }
        }
    }

    /**
     * Converts the given template to HTML with an optional formatter
     */
    fun toHtml(formatter: HtmlFormatter = HtmlFormatter()): String = toString(formatter)

    /**
     * Appends the HTML representation of this template to the given appendable
     */
    fun appendHtml(buffer: Appendable, formatter: HtmlFormatter = HtmlFormatter()): String = toString(formatter)
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
