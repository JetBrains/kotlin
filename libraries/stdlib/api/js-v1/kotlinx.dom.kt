/*∆*/ @kotlin.SinceKotlin(version = "1.4")
/*∆*/ public val org.w3c.dom.Node.isElement: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/ @kotlin.SinceKotlin(version = "1.4")
/*∆*/ public val org.w3c.dom.Node.isText: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/ @kotlin.SinceKotlin(version = "1.4")
/*∆*/ public fun org.w3c.dom.Element.addClass(vararg cssClasses: kotlin.String): kotlin.Boolean
/*∆*/ 
/*∆*/ @kotlin.SinceKotlin(version = "1.4")
/*∆*/ public fun org.w3c.dom.Element.appendElement(name: kotlin.String, init: org.w3c.dom.Element.() -> kotlin.Unit): org.w3c.dom.Element
/*∆*/ 
/*∆*/ @kotlin.SinceKotlin(version = "1.4")
/*∆*/ public fun org.w3c.dom.Element.appendText(text: kotlin.String): org.w3c.dom.Element
/*∆*/ 
/*∆*/ @kotlin.SinceKotlin(version = "1.4")
/*∆*/ public fun org.w3c.dom.Node.clear(): kotlin.Unit
/*∆*/ 
/*∆*/ @kotlin.SinceKotlin(version = "1.4")
/*∆*/ public fun org.w3c.dom.Document.createElement(name: kotlin.String, init: org.w3c.dom.Element.() -> kotlin.Unit): org.w3c.dom.Element
/*∆*/ 
/*∆*/ @kotlin.SinceKotlin(version = "1.4")
/*∆*/ public fun org.w3c.dom.Element.hasClass(cssClass: kotlin.String): kotlin.Boolean
/*∆*/ 
/*∆*/ @kotlin.SinceKotlin(version = "1.4")
/*∆*/ public fun org.w3c.dom.Element.removeClass(vararg cssClasses: kotlin.String): kotlin.Boolean