package js.jquery;

import js.dom.core.Element

native
class JQuery() {
    fun addClass(className : String) : JQuery = js.noImpl;
    fun addClass(f : Element.(Int, String)->String) = js.noImpl;

    fun attr(attrName : String) = "";
    fun attr(attrName : String, value : String) = this;

    fun html() : String = "";
    fun html(s : String) = this;
    fun html(f : Element.(Int, String)->String) = this;


    fun hasClass(className : String) = true
    fun removeClass(className : String) = this
    fun height() = 0
    fun width() = 0

    fun click() = this;

    fun mousedown(handler : Element.(MouseEvent)->Unit) = this;
    fun mouseup(handler : Element.(MouseEvent)->Unit) = this;
    fun mousemove(handler : Element.(MouseEvent)->Unit) = this;

    fun dblclick(handler : Element.(MouseClickEvent)->Unit) = this;
    fun click(handler : Element.(MouseClickEvent)->Unit) = this;

    fun load(handler : Element.()->Unit) = this;
    fun change(handler : Element.()->Unit) = this;

    fun append(str : String) = this;
    fun ready(handler : ()->Unit) = this;
    fun text(text : String) = this;
    fun slideUp() = this;
    fun hover(handlerInOut : Element.() -> Unit) = this;
    fun hover(handlerIn : Element.() -> Unit, handlerOut : Element.() -> Unit) = this;
    fun next() : JQuery = js.noImpl
    fun parent() : JQuery = js.noImpl
    fun `val`() : String? = js.noImpl
}

native
open class MouseEvent() {
    val pageX : Double = 0.0;
    val pageY : Double = 0.0;
    fun preventDefault() {}
    fun isDefaultPrevented() : Boolean = true;
}

native
class MouseClickEvent() : MouseEvent() {
    val which : Int = 0;
}



native("$")
fun jq(selector : String) = JQuery();
native("$")
fun jq(selector : String, context : Element) = JQuery();
native("$")
fun jq(callback : () -> Unit) = JQuery();
native("$")
fun jq(obj : JQuery) = JQuery();
native("$")
fun jq(el : Element) = JQuery();
native("$")
fun jq() = JQuery();
