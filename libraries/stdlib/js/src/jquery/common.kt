@file: Suppress("DEPRECATION")
package jquery

import org.w3c.dom.Element

@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
public external class JQuery() {
    public fun addClass(className: String): JQuery
    public fun addClass(f: (Int, String) -> String): JQuery

    public fun attr(attrName: String): String
    public fun attr(attrName: String, value: String): JQuery

    public fun html(): String
    public fun html(s: String): JQuery
    public fun html(f: (Int, String) -> String): JQuery


    public fun hasClass(className: String): Boolean
    public fun removeClass(className: String): JQuery
    public fun height(): Number
    public fun width(): Number

    public fun click(): JQuery

    public fun mousedown(handler: (MouseEvent) -> Unit): JQuery
    public fun mouseup(handler: (MouseEvent) -> Unit): JQuery
    public fun mousemove(handler: (MouseEvent) -> Unit): JQuery

    public fun dblclick(handler: (MouseClickEvent) -> Unit): JQuery
    public fun click(handler: (MouseClickEvent) -> Unit): JQuery

    public fun load(handler: () -> Unit): JQuery
    public fun change(handler: () -> Unit): JQuery

    public fun append(str: String): JQuery
    public fun ready(handler: () -> Unit): JQuery
    public fun text(text: String): JQuery
    public fun slideUp(): JQuery
    public fun hover(handlerInOut: () -> Unit): JQuery
    public fun hover(handlerIn: () -> Unit, handlerOut: () -> Unit): JQuery
    public fun next(): JQuery
    public fun parent(): JQuery
    public fun `val`(): String?
}

@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
open public external class MouseEvent() {
    public val pageX: Double
    public val pageY: Double
    public fun preventDefault()
    public fun isDefaultPrevented(): Boolean
}

@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
public external class MouseClickEvent() : MouseEvent {
    public val which: Int
}

@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
@JsName("$")
public external fun jq(selector: String): JQuery
@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
@JsName("$")
public external fun jq(selector: String, context: Element): JQuery
@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
@JsName("$")
public external fun jq(callback: () -> Unit): JQuery
@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
@JsName("$")
public external fun jq(obj: JQuery): JQuery
@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
@JsName("$")
public external fun jq(el: Element): JQuery
@Deprecated("Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.")
@JsName("$")
public external fun jq(): JQuery
