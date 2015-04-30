package jquery

import org.w3c.dom.Element

native
public class JQuery() {
    public fun addClass(className: String): JQuery = noImpl;
    public fun addClass(f: Element.(Int, String) -> String): JQuery = noImpl;

    public fun attr(attrName: String): String = "";
    public fun attr(attrName: String, value: String): JQuery = this;

    public fun html(): String = "";
    public fun html(s: String): JQuery = this;
    public fun html(f: Element.(Int, String) -> String): JQuery = this;


    public fun hasClass(className: String): Boolean = true
    public fun removeClass(className: String): JQuery = this
    public fun height(): Number = 0
    public fun width(): Number = 0

    public fun click(): JQuery = this;

    public fun mousedown(handler: Element.(MouseEvent) -> Unit): JQuery = this;
    public fun mouseup(handler: Element.(MouseEvent) -> Unit): JQuery = this;
    public fun mousemove(handler: Element.(MouseEvent) -> Unit): JQuery = this;

    public fun dblclick(handler: Element.(MouseClickEvent) -> Unit): JQuery = this;
    public fun click(handler: Element.(MouseClickEvent) -> Unit): JQuery = this;

    public fun load(handler: Element.() -> Unit): JQuery = this;
    public fun change(handler: Element.() -> Unit): JQuery = this;

    public fun append(str: String): JQuery = this;
    public fun ready(handler: () -> Unit): JQuery = this;
    public fun text(text: String): JQuery = this;
    public fun slideUp(): JQuery = this;
    public fun hover(handlerInOut: Element.() -> Unit): JQuery = this;
    public fun hover(handlerIn: Element.() -> Unit, handlerOut: Element.() -> Unit): JQuery = this;
    public fun next(): JQuery = noImpl
    public fun parent(): JQuery = noImpl
    public fun `val`(): String? = noImpl
}

native
open public class MouseEvent() {
    public val pageX: Double = 0.0;
    public val pageY: Double = 0.0;
    public fun preventDefault() {
    }
    public fun isDefaultPrevented(): Boolean = true;
}

native
public class MouseClickEvent() : MouseEvent() {
    public val which: Int = 0;
}

native("$")
public fun jq(selector: String): JQuery = JQuery();
native("$")
public fun jq(selector: String, context: Element): JQuery = JQuery();
native("$")
native("$")
public fun jq(callback: () -> Unit): JQuery = JQuery();
native("$")
public fun jq(obj: JQuery): JQuery = JQuery();
native("$")
public fun jq(el: Element): JQuery = JQuery();
native("$")
native("$")
public fun jq(): JQuery = JQuery();
