package kotlin.template.experiment1

import org.w3c.dom.Node
import kotlin.dom.toXmlString

import java.lang.Number
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

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


abstract class TextBuilder(val output : Appendable) {

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

    fun text(value : String) : Unit {
        output.append(value)
    }
}

open class I18nBuilder(output : Appendable, val formatter : I18nFormatter) : TextBuilder(output) {

    fun expression(value : Any?) : Unit {
        escape(formatter.format(value))
    }

    fun expression(value : Number) : Unit {
        escape(formatter.formatNumber(value))
    }

    fun expression(value : Date) : Unit {
        escape(formatter.formatDate(value))
    }

    /**
     * By default does no escaping but inherited classes may escape text
     */
    open fun escape(text : String) : Unit {
        output.append(text)
    }
}

open class HtmlBuilder(output : Appendable, formatter : I18nFormatter) : I18nBuilder(output, formatter) {

    /**
     * Nodes are assumed to already be properly HTML/XML encoded so don't escape
     */
    fun expression(value : Node) : Unit {
        text(value.toXmlString())
    }

    /**
     * Escapes text using HTML escaping of sensitive tokens
     */
    override fun escape(text : String) : Unit {
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
    fun format(val value : Any?) : String
}

open class ToStringFormatter : ValueFormatter {

    var nullText : String = "null"

    open fun toString() = "ToStringFormatter"

    override fun format(value : Any?): String {
        return if (value == null) nullText else value.toString()
    }
}

open class I18nFormatter(val locale : Locale = Locale.getDefault().sure()): ToStringFormatter() {

    override fun toString() = "I18nFormatter"

    public var numberFormat : NumberFormat = NumberFormat.getInstance(locale).sure()

    public var dateFormat : DateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, locale).sure()

    fun formatNumber(number : Number): String {
        return numberFormat.format(number) ?: ""
    }

    fun formatDate(date : Date): String {
        return dateFormat.format(date) ?: ""
    }

    /**
     * Generates the internationalised text for this formatter
     */
    fun toString(fn : (I18nBuilder) -> Unit) : String {
        val buffer = StringBuilder()
        val builder = I18nBuilder(buffer, this)
        fn(builder)
        return buffer.toString() ?: ""
    }

    /**
     * Generates the HTML for the given function
     */
    fun toHtml(fn : (HtmlBuilder) -> Unit) : String {
        val buffer = StringBuilder()
        val html = HtmlBuilder(buffer, this)
        fn(html)
        return buffer.toString() ?: ""
    }
}




