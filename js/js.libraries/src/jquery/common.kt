package jquery

import org.w3c.dom.Element

public external class JQuery() {
    public fun addClass(className: String): JQuery = noImpl;
    public fun addClass(f: Element.(Int, String) -> String): JQuery = noImpl;

    public fun attr(attrName: String): String
    public fun attr(attrName: String, value: String): JQuery

    public fun html(): String
    public fun html(s: String): JQuery
    public fun html(f: Element.(Int, String) -> String): JQuery


    public fun hasClass(className: String): Boolean
    public fun removeClass(className: String): JQuery
    public fun height(): Number
    public fun width(): Number

    public fun click(): JQuery

    public fun mousedown(handler: Element.(MouseEvent) -> Unit): JQuery
    public fun mouseup(handler: Element.(MouseEvent) -> Unit): JQuery
    public fun mousemove(handler: Element.(MouseEvent) -> Unit): JQuery

    public fun dblclick(handler: Element.(MouseClickEvent) -> Unit): JQuery
    public fun click(handler: Element.(MouseClickEvent) -> Unit): JQuery

    public fun load(handler: Element.() -> Unit): JQuery
    public fun change(handler: Element.() -> Unit): JQuery

    public fun append(str: String): JQuery
    public fun ready(handler: () -> Unit): JQuery
    public fun text(text: String): JQuery
    public fun slideUp(): JQuery
    public fun hover(handlerInOut: Element.() -> Unit): JQuery
    public fun hover(handlerIn: Element.() -> Unit, handlerOut: Element.() -> Unit): JQuery
    public fun next(): JQuery = noImpl
    public fun parent(): JQuery = noImpl
    public fun `val`(): String? = noImpl
}

open public external class MouseEvent() {
    public val pageX: Double
    public val pageY: Double
    public fun preventDefault()
    public fun isDefaultPrevented(): Boolean
}

public external class MouseClickEvent() : MouseEvent {
    public val which: Int
}

@JsName("$")
public external fun jq(selector: String): JQuery
@JsName("$")
public external fun jq(selector: String, context: Element): JQuery
@JsName("$")
public external fun jq(callback: () -> Unit): JQuery
@JsName("$")
public external fun jq(obj: JQuery): JQuery
@JsName("$")
public external fun jq(el: Element): JQuery
@JsName("$")
public external fun jq(): JQuery
