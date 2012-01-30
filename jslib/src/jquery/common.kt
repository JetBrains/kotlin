package jquery;

import js.annotations.*;

fun JQuery.toString() = ""

NativeClass
class JQuery() {
    fun addClass(className : String) : JQuery = this;
    fun addClass(f : DomElement.(Int, String)->String) = this;
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

NativeFun("$")
fun jq(selector : String) = JQuery();
NativeFun("$")
fun jq(selector : String, context : DomElement) = JQuery();
NativeFun("$")
fun jq(callback : () -> Unit) = JQuery();
NativeFun("$")
fun jq(obj : JQuery) = JQuery();
NativeFun("$")
fun jq(el : DomElement) = JQuery();
NativeFun("$")
fun jq() = JQuery();
