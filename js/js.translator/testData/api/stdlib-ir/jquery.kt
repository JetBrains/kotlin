package jquery

@kotlin.Deprecated(message = "Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.") @kotlin.js.JsName(name = "$") public external fun jq(): jquery.JQuery
@kotlin.Deprecated(message = "Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.") @kotlin.js.JsName(name = "$") public external fun jq(/*0*/ callback: () -> kotlin.Unit): jquery.JQuery
@kotlin.Deprecated(message = "Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.") @kotlin.js.JsName(name = "$") public external fun jq(/*0*/ obj: jquery.JQuery): jquery.JQuery
@kotlin.Deprecated(message = "Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.") @kotlin.js.JsName(name = "$") public external fun jq(/*0*/ selector: kotlin.String): jquery.JQuery
@kotlin.Deprecated(message = "Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.") @kotlin.js.JsName(name = "$") public external fun jq(/*0*/ selector: kotlin.String, /*1*/ context: org.w3c.dom.Element): jquery.JQuery
@kotlin.Deprecated(message = "Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.") @kotlin.js.JsName(name = "$") public external fun jq(/*0*/ el: org.w3c.dom.Element): jquery.JQuery

@kotlin.Deprecated(message = "Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.") public final external class JQuery {
    /*primary*/ public constructor JQuery()
    public final fun addClass(/*0*/ f: (kotlin.Int, kotlin.String) -> kotlin.String): jquery.JQuery
    public final fun addClass(/*0*/ className: kotlin.String): jquery.JQuery
    public final fun append(/*0*/ str: kotlin.String): jquery.JQuery
    public final fun attr(/*0*/ attrName: kotlin.String): kotlin.String
    public final fun attr(/*0*/ attrName: kotlin.String, /*1*/ value: kotlin.String): jquery.JQuery
    public final fun change(/*0*/ handler: () -> kotlin.Unit): jquery.JQuery
    public final fun click(): jquery.JQuery
    public final fun click(/*0*/ handler: (jquery.MouseClickEvent) -> kotlin.Unit): jquery.JQuery
    public final fun dblclick(/*0*/ handler: (jquery.MouseClickEvent) -> kotlin.Unit): jquery.JQuery
    public final fun hasClass(/*0*/ className: kotlin.String): kotlin.Boolean
    public final fun height(): kotlin.Number
    public final fun hover(/*0*/ handlerInOut: () -> kotlin.Unit): jquery.JQuery
    public final fun hover(/*0*/ handlerIn: () -> kotlin.Unit, /*1*/ handlerOut: () -> kotlin.Unit): jquery.JQuery
    public final fun html(): kotlin.String
    public final fun html(/*0*/ f: (kotlin.Int, kotlin.String) -> kotlin.String): jquery.JQuery
    public final fun html(/*0*/ s: kotlin.String): jquery.JQuery
    public final fun load(/*0*/ handler: () -> kotlin.Unit): jquery.JQuery
    public final fun mousedown(/*0*/ handler: (jquery.MouseEvent) -> kotlin.Unit): jquery.JQuery
    public final fun mousemove(/*0*/ handler: (jquery.MouseEvent) -> kotlin.Unit): jquery.JQuery
    public final fun mouseup(/*0*/ handler: (jquery.MouseEvent) -> kotlin.Unit): jquery.JQuery
    public final fun next(): jquery.JQuery
    public final fun parent(): jquery.JQuery
    public final fun ready(/*0*/ handler: () -> kotlin.Unit): jquery.JQuery
    public final fun removeClass(/*0*/ className: kotlin.String): jquery.JQuery
    public final fun slideUp(): jquery.JQuery
    public final fun text(/*0*/ text: kotlin.String): jquery.JQuery
    public final fun `val`(): kotlin.String?
    public final fun width(): kotlin.Number
}

@kotlin.Deprecated(message = "Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.") public final external class MouseClickEvent : jquery.MouseEvent {
    /*primary*/ public constructor MouseClickEvent()
    public final val which: kotlin.Int
        public final fun <get-which>(): kotlin.Int
}

@kotlin.Deprecated(message = "Use declarations from 'https://bintray.com/kotlin/js-externals/kotlin-js-jquery' package instead.") public open external class MouseEvent {
    /*primary*/ public constructor MouseEvent()
    public final val pageX: kotlin.Double
        public final fun <get-pageX>(): kotlin.Double
    public final val pageY: kotlin.Double
        public final fun <get-pageY>(): kotlin.Double
    public final fun isDefaultPrevented(): kotlin.Boolean
    public final fun preventDefault(): kotlin.Unit
}