package kotlin.dom

public val org.w3c.dom.Node.isElement: kotlin.Boolean
    public fun org.w3c.dom.Node.<get-isElement>(): kotlin.Boolean
public val org.w3c.dom.Node.isText: kotlin.Boolean
    public fun org.w3c.dom.Node.<get-isText>(): kotlin.Boolean
public fun org.w3c.dom.Element.addClass(/*0*/ vararg cssClasses: kotlin.String /*kotlin.Array<out kotlin.String>*/): kotlin.Boolean
public fun org.w3c.dom.Element.appendElement(/*0*/ name: kotlin.String, /*1*/ init: org.w3c.dom.Element.() -> kotlin.Unit): org.w3c.dom.Element
public fun org.w3c.dom.Element.appendText(/*0*/ text: kotlin.String): org.w3c.dom.Element
public fun org.w3c.dom.Node.clear(): kotlin.Unit
public fun org.w3c.dom.Document.createElement(/*0*/ name: kotlin.String, /*1*/ init: org.w3c.dom.Element.() -> kotlin.Unit): org.w3c.dom.Element
public fun org.w3c.dom.Element.hasClass(/*0*/ cssClass: kotlin.String): kotlin.Boolean
public fun org.w3c.dom.Element.removeClass(/*0*/ vararg cssClasses: kotlin.String /*kotlin.Array<out kotlin.String>*/): kotlin.Boolean