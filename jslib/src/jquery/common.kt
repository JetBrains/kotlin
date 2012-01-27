package jquery;

fun fail() : Nothing = throw RuntimeException();

fun JQuery.toString() = ""

class JQuery() {
    fun addClass(className : String) : JQuery = fail();
    fun attr(attrName : String) = ""
    fun attr(attrName : String, value : String) = this
    fun hasClass(attrName : String) = true
    fun html() : String = ""
    fun height() = 0
    fun width() = 0
    fun click() = this;
    fun click(handler : (DomElement)->Unit) = this;
    fun append(str : String) = this;
}

class DomElement() {
}

fun jq(selector : String) = JQuery();
fun jq(selector : String, context : DomElement) = JQuery();
fun jq(callback : () -> Unit) = JQuery();
fun jq(obj : JQuery) = JQuery();
