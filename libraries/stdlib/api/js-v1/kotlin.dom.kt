@kotlin.internal.LowPriorityInOverloadResolution
@kotlin.Deprecated(message = "This API is moved to another package, use 'kotlinx.dom.isElement' instead.", replaceWith = kotlin.ReplaceWith(expression = "this.isElement", imports = {"kotlinx.dom.isElement"}))
public val org.w3c.dom.Node.isElement: kotlin.Boolean { get; }

@kotlin.internal.LowPriorityInOverloadResolution
@kotlin.Deprecated(message = "This API is moved to another package, use 'kotlinx.dom.isText' instead.", replaceWith = kotlin.ReplaceWith(expression = "this.isText", imports = {"kotlinx.dom.isText"}))
public val org.w3c.dom.Node.isText: kotlin.Boolean { get; }

@kotlin.internal.LowPriorityInOverloadResolution
@kotlin.Deprecated(message = "This API is moved to another package, use 'kotlinx.dom.addClass' instead.", replaceWith = kotlin.ReplaceWith(expression = "this.addClass(cssClasses)", imports = {"kotlinx.dom.addClass"}))
public inline fun org.w3c.dom.Element.addClass(vararg cssClasses: kotlin.String): kotlin.Boolean

@kotlin.internal.LowPriorityInOverloadResolution
@kotlin.Deprecated(message = "This API is moved to another package, use 'kotlinx.dom.appendElement' instead.", replaceWith = kotlin.ReplaceWith(expression = "this.appendElement(name, init)", imports = {"kotlinx.dom.appendElement"}))
public inline fun org.w3c.dom.Element.appendElement(name: kotlin.String, noinline init: org.w3c.dom.Element.() -> kotlin.Unit): org.w3c.dom.Element

@kotlin.internal.LowPriorityInOverloadResolution
@kotlin.Deprecated(message = "This API is moved to another package, use 'kotlinx.dom.appendText' instead.", replaceWith = kotlin.ReplaceWith(expression = "this.appendText(text)", imports = {"kotlinx.dom.appendText"}))
public inline fun org.w3c.dom.Element.appendText(text: kotlin.String): org.w3c.dom.Element

@kotlin.internal.LowPriorityInOverloadResolution
@kotlin.Deprecated(message = "This API is moved to another package, use 'kotlinx.dom.clear' instead.", replaceWith = kotlin.ReplaceWith(expression = "this.clear()", imports = {"kotlinx.dom.clear"}))
public inline fun org.w3c.dom.Node.clear(): kotlin.Unit

@kotlin.internal.LowPriorityInOverloadResolution
@kotlin.Deprecated(message = "This API is moved to another package, use 'kotlinx.dom.createElement' instead.", replaceWith = kotlin.ReplaceWith(expression = "this.createElement(name, init)", imports = {"kotlinx.dom.createElement"}))
public inline fun org.w3c.dom.Document.createElement(name: kotlin.String, noinline init: org.w3c.dom.Element.() -> kotlin.Unit): org.w3c.dom.Element

@kotlin.internal.LowPriorityInOverloadResolution
@kotlin.Deprecated(message = "This API is moved to another package, use 'kotlinx.dom.hasClass' instead.", replaceWith = kotlin.ReplaceWith(expression = "this.hasClass(cssClass)", imports = {"kotlinx.dom.hasClass"}))
public inline fun org.w3c.dom.Element.hasClass(cssClass: kotlin.String): kotlin.Boolean

@kotlin.internal.LowPriorityInOverloadResolution
@kotlin.Deprecated(message = "This API is moved to another package, use 'kotlinx.dom.removeClass' instead.", replaceWith = kotlin.ReplaceWith(expression = "this.removeClass(cssClasses)", imports = {"kotlinx.dom.removeClass"}))
public inline fun org.w3c.dom.Element.removeClass(vararg cssClasses: kotlin.String): kotlin.Boolean