package kotlin.template

import kotlin.dom.toXmlString
import org.w3c.dom.Node


/**
 * Creates a string template from a string with $ expressions inside using HTML escaping of expressions.
 */
// TODO varargs on constructors seems to fail
//class HtmlTemplate(vararg val text: String) {
class HtmlTemplate(val constantText : Array<String>) {

    /**
     * Creates a builder of string expressions which use HTML encoding on expressions
     */
    fun builder() : HtmlTemplateBuilder {
        // TODO we should allow the caller to pass these in when we create the builder!
        val options = HtmlTemplateOptions()
        return HtmlTemplateBuilder(constantText, options)
    }
}

class HtmlTemplateOptions() {
    var nullText : String = ""
}

open class HtmlTemplateBuilder(constantText : Array<String>, val options : HtmlTemplateOptions) : StringTemplateBuilder(constantText) {

    override fun expression(expression : Any) : Unit {
        val text = if (expression != null) expression.toString() ?: "" else options.nullText
        escape(text)
    }

    /**
     * Appends the DOM node, no HTML escaping is done as we assume its already escaped
     */
    fun expression(node : Node) : Unit {
        // no need to escape
        unescape(node.toXmlString(false))
    }

    /**
     * Appends the given text escaped properly
     */
    fun escape(text : String): Unit {
        appendNextConstant()
        for (c in text) {
            if (c == '<') buffer.append("&lt;")
            else if (c == '>') buffer.append("&gt;")
            else if (c == '&') buffer.append("&amp;")
            else if (c == '"') buffer.append("&quot;")
            else buffer.append(c)
        }
    }

    /**
     * Appends the unescaped text
     */
    fun unescape(text : String) {
        appendNextConstant()
        buffer.append(text)
    }
}
