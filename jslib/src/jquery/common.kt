package jquery;

import js.annotations.*;

Native
class JQuery() {
    fun addClass(className : String) : JQuery = this;
    fun addClass(f : DomElement.(Int, String)->String) = this;

    fun attr(attrName : String) = "";
    fun attr(attrName : String, value : String) = this;

    fun html() : String = "";
    fun html(s : String) = this;
    fun html(f : DomElement.(Int, String)->String) = this;


    fun hasClass(className : String) = true
    fun removeClass(className : String) = this
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

Native("$")
fun jq(selector : String) = JQuery();
Native("$")
fun jq(selector : String, context : DomElement) = JQuery();
Native("$")
fun jq(callback : () -> Unit) = JQuery();
Native("$")
fun jq(obj : JQuery) = JQuery();
Native("$")
fun jq(el : DomElement) = JQuery();
Native("$")
fun jq() = JQuery();
