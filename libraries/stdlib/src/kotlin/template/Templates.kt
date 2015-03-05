package kotlin.template

import kotlin.dom. *
import org.w3c.dom.Node
import com.sun.org.apache.xalan.internal.xsltc.dom.UnionIterator
import java.util.Locale
import java.text.NumberFormat
import java.text.DateFormat
import java.util.Date

// TODO this class should move into the runtime
// in kotlin.StringTemplate
deprecated("This class is part of an experimental implementation of string templates and is going to be removed")
public class StringTemplate(private val values: Array<Any?>) {

    /**
     * Converts the template into a String
     */
    override fun toString(): String {
        val out = StringBuilder()
        forEach { out.append(it) }
        return out.toString()
    }

    /**
     * Performs the given function on each value in the collection
     */
    public fun forEach(fn: (Any?) -> Unit): Unit {
        for (v in values) {
            fn(v)
        }
    }
}

/**
 * Converts the string template into a string using the given formatter
 * to encode values as Strings performing any special encoding (such as for HTML)
 * or internationalisation.
 *
 * See [[HtmlFormatter] and [[LocaleFormatter] respectively.
 */
deprecated("This function is part of an experimental implementation of string templates and is going to be removed")
public fun StringTemplate.toString(formatter: Formatter): String {
    val buffer = StringBuilder()
    append(buffer, formatter)
    return buffer.toString()
}

/**
 * Appends the text representation of this string template to the given output
 * using the supplied formatter
 */
deprecated("This function is part of an experimental implementation of string templates and is going to be removed")
public fun StringTemplate.append(out: Appendable, formatter: Formatter): Unit {
    var constantText = true
    this.forEach {
        if (constantText) {
            if (it == null) {
                throw IllegalStateException("No constant checks should be null");
            } else {
                val text = it.toString()
                out.append(text)
            }
        } else {
            formatter.format(out, it)
        }
        constantText = !constantText
    }
}

/**
 * Converts this string template to internationalised text using the supplied
 * [[LocaleFormatter]]
 */
deprecated("This function is part of an experimental implementation of string templates and is going to be removed")
public fun StringTemplate.toLocale(formatter: LocaleFormatter = LocaleFormatter()): String = toString(formatter)

/**
 * Converts this string template to HTML text
 */
deprecated("This function is part of an experimental implementation of string templates and is going to be removed")
public fun StringTemplate.toHtml(formatter: HtmlFormatter = HtmlFormatter()): String = toString(formatter)

/**
 * Represents a formatter and encoder of values in a [[StringTemplate]] which understands
 * how to format values for a particular [[Locale]] such as with the [[LocaleFormatter]] or
 * to escape particular characters in different output formats such as [[HtmlFormatter]
 */
deprecated("This trait is part of an experimental implementation of string templates and is going to be removed")
public trait Formatter {
    public fun format(buffer: Appendable, value: Any?): Unit
}

/**
 * Formats strings with no special encoding other than allowing the null text to be
 * configured
 */
deprecated("This class is part of an experimental implementation of string templates and is going to be removed")
public open class ToStringFormatter : Formatter {

    private val nullString: String = "null"

    override fun toString(): String = "ToStringFormatter"

    public override fun format(out: Appendable, value: Any?) {
        if (value == null) {
            format(out, nullString)
        } else if (value is StringTemplate) {
            value.append(out, this)
        } else {
            format(out, value.toString())
        }
    }

    /**
     * Formats the given string allowing derived classes to override this method
     * to escape strings with special characters such as for HTML
     */
    public open fun format(out: Appendable, text: String): Unit {
        out.append(text)
    }
}

deprecated("This property is part of an experimental implementation of string templates and is going to be removed")
public val defaultLocale: Locale = Locale.getDefault()

/**
 * Formats values using a given [[Locale]] for internationalisation
 */
deprecated("This class is part of an experimental implementation of string templates and is going to be removed")
public open class LocaleFormatter(protected val locale: Locale = defaultLocale) : ToStringFormatter() {

    override fun toString(): String = "LocaleFormatter{$locale}"

    public var numberFormat: NumberFormat = NumberFormat.getInstance(locale)!!

    public var dateFormat: DateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, locale)!!

    public override fun format(out: Appendable, value: Any?) {
        if (value is Number) {
            format(out, format(value))
        } else if (value is Date) {
            format(out, format(value))
        } else {
            super.format(out, value)
        }
    }

    public fun format(number: Number): String {
        return numberFormat.format(number) ?: ""
    }

    public fun format(date: Date): String {
        return dateFormat.format(date) ?: ""
    }
}

/**
 * Formats values for HTML encoding, escaping special characters in HTML.
 */
deprecated("This class is part of an experimental implementation of string templates and is going to be removed")
public class HtmlFormatter(locale: Locale = defaultLocale) : LocaleFormatter(locale) {

    override fun toString(): String = "HtmlFormatter{$locale}"

    public override fun format(out: Appendable, value: Any?) {
        if (value is Node) {
            out.append(value.toXmlString())
        } else {
            super.format(out, value)
        }
    }

    public override fun format(buffer: Appendable, text: String): Unit {
        for (c in text) {
            if (c == '<') buffer.append("&lt;")
            else if (c == '>') buffer.append("&gt;")
            else if (c == '&') buffer.append("&amp;")
            else if (c == '"') buffer.append("&quot;")
            else buffer.append(c)
        }
    }
}


