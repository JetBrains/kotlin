@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.")
@kotlin.js.JsName(name = "$")
public external fun jq(): jquery.JQuery

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.")
@kotlin.js.JsName(name = "$")
public external fun jq(callback: () -> kotlin.Unit): jquery.JQuery

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.")
@kotlin.js.JsName(name = "$")
public external fun jq(obj: jquery.JQuery): jquery.JQuery

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.")
@kotlin.js.JsName(name = "$")
public external fun jq(selector: kotlin.String): jquery.JQuery

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.")
@kotlin.js.JsName(name = "$")
public external fun jq(selector: kotlin.String, context: org.w3c.dom.Element): jquery.JQuery

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.")
@kotlin.js.JsName(name = "$")
public external fun jq(el: org.w3c.dom.Element): jquery.JQuery

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.")
public final external class JQuery {
    public constructor JQuery()

    public final fun addClass(f: (kotlin.Int, kotlin.String) -> kotlin.String): jquery.JQuery

    public final fun addClass(className: kotlin.String): jquery.JQuery

    public final fun append(str: kotlin.String): jquery.JQuery

    public final fun attr(attrName: kotlin.String): kotlin.String

    public final fun attr(attrName: kotlin.String, value: kotlin.String): jquery.JQuery

    public final fun change(handler: () -> kotlin.Unit): jquery.JQuery

    public final fun click(): jquery.JQuery

    public final fun click(handler: (jquery.MouseClickEvent) -> kotlin.Unit): jquery.JQuery

    public final fun dblclick(handler: (jquery.MouseClickEvent) -> kotlin.Unit): jquery.JQuery

    public final fun hasClass(className: kotlin.String): kotlin.Boolean

    public final fun height(): kotlin.Number

    public final fun hover(handlerInOut: () -> kotlin.Unit): jquery.JQuery

    public final fun hover(handlerIn: () -> kotlin.Unit, handlerOut: () -> kotlin.Unit): jquery.JQuery

    public final fun html(): kotlin.String

    public final fun html(f: (kotlin.Int, kotlin.String) -> kotlin.String): jquery.JQuery

    public final fun html(s: kotlin.String): jquery.JQuery

    public final fun load(handler: () -> kotlin.Unit): jquery.JQuery

    public final fun mousedown(handler: (jquery.MouseEvent) -> kotlin.Unit): jquery.JQuery

    public final fun mousemove(handler: (jquery.MouseEvent) -> kotlin.Unit): jquery.JQuery

    public final fun mouseup(handler: (jquery.MouseEvent) -> kotlin.Unit): jquery.JQuery

    public final fun next(): jquery.JQuery

    public final fun parent(): jquery.JQuery

    public final fun ready(handler: () -> kotlin.Unit): jquery.JQuery

    public final fun removeClass(className: kotlin.String): jquery.JQuery

    public final fun slideUp(): jquery.JQuery

    public final fun text(text: kotlin.String): jquery.JQuery

    public final fun `val`(): kotlin.String?

    public final fun width(): kotlin.Number
}

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.")
public final external class MouseClickEvent : jquery.MouseEvent {
    public constructor MouseClickEvent()

    public final val which: kotlin.Int { get; }
}

@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Use declarations from 'https://maven.pkg.jetbrains.space/kotlin/p/kotlin/js-externals/kotlin/js/externals/kotlin-js-jquery/' package instead.")
public open external class MouseEvent {
    public constructor MouseEvent()

    public final val pageX: kotlin.Double { get; }

    public final val pageY: kotlin.Double { get; }

    public final fun isDefaultPrevented(): kotlin.Boolean

    public final fun preventDefault(): kotlin.Unit
}