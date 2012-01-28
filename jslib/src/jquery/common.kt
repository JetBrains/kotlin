package jquery;

fun fail() : Nothing = throw RuntimeException();
fun JQuery.toString() = ""

class JQuery() {
    fun addClass(className : String) : JQuery = fail();
    fun attr(attrName : String) = ""
    fun attr(attrName : String, value : String) = this
    fun hasClass(className : String) = true
    fun removeClass(className : String) = this
    fun html() : String = ""
    fun height() = 0
    fun width() = 0
    fun click() = this;
    fun click(handler : DomElement.()->Unit) = this;
    fun append(str : String) = this;
    fun ready(handler : ()->Unit) = this;
    fun text(text : String) = this;
    fun slideUp() = this;
    fun hover(handlerInOut : DomElement.() -> Unit) = this;
    fun hover(handlerIn : DomElement.() -> Unit, handlerOut : DomElement.() -> Unit) = this;
}

open class DomElement() {

}

//val document = object : DomElement() {}

fun jq(selector : String) = JQuery();
fun jq(selector : String, context : DomElement) = JQuery();
fun jq(callback : () -> Unit) = JQuery();
fun jq(obj : JQuery) = JQuery();
fun jq(el : DomElement) = JQuery();
fun jq() = JQuery();
