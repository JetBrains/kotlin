package js.jquery;

import js.dom.core.Element

native
public public class JQuery() {
    public fun addClass(className : String) : JQuery = js.noImpl;
    public fun addClass(f : Element.(Int, String)->String) = js.noImpl;

    public fun attr(attrName : String) = "";
    public fun attr(attrName : String, value : String) = this;

    public fun html() : String = "";
    public fun html(s : String) = this;
    public fun html(f : Element.(Int, String)->String) = this;


    public fun hasClass(className : String) = true
    public fun removeClass(className : String) = this
    public fun height() = 0
    public fun width() = 0

    public fun click() = this;

    public fun mousedown(handler : Element.(MouseEvent)->Unit) = this;
    public fun mouseup(handler : Element.(MouseEvent)->Unit) = this;
    public fun mousemove(handler : Element.(MouseEvent)->Unit) = this;

    public fun dblclick(handler : Element.(MouseClickEvent)->Unit) = this;
    public fun click(handler : Element.(MouseClickEvent)->Unit) = this;

    public fun load(handler : Element.()->Unit) = this;
    public fun change(handler : Element.()->Unit) = this;

    public fun append(str : String) = this;
    public fun ready(handler : ()->Unit) = this;
    public fun text(text : String) = this;
    public fun slideUp() = this;
    public fun hover(handlerInOut : Element.() -> Unit) = this;
    public fun hover(handlerIn : Element.() -> Unit, handlerOut : Element.() -> Unit) = this;
    public fun next() : JQuery = js.noImpl
    public fun parent() : JQuery = js.noImpl
    public fun `val`() : String? = js.noImpl
}

native
open public class MouseEvent() {
    public val pageX : Double = 0.0;
    public val pageY : Double = 0.0;
    public fun preventDefault() {}
    public fun isDefaultPrevented() : Boolean = true;
}

native
public class MouseClickEvent() : MouseEvent() {
    val which : Int = 0;
}



native("$")
public fun jq(selector : String) = JQuery();
native("$")
public fun jq(selector : String, context : Element) = JQuery();
native("$")
public fun jq(callback : () -> Unit) = JQuery();
native("$")
public fun jq(obj : JQuery) = JQuery();
native("$")
public fun jq(el : Element) = JQuery();
native("$")
public fun jq() = JQuery();
