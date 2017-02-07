package kotlin.dom.build

import org.w3c.dom.*
import kotlin.dom.*

/**
 * Creates a new element which can be configured via a function
 */
@Deprecated("Moved to kotlin.dom package", ReplaceWith("createElement(name, init)", "kotlin.dom.createElement"), level = DeprecationLevel.ERROR)
public fun Document.createElement(name: String, init: Element.() -> Unit): Element = createElement(name).apply(init)

/**
 * Creates a new element to an element which has an owner Document which can be configured via a function
 */
@Deprecated("Use Document.createElement instead", level = DeprecationLevel.ERROR)
fun Element.createElement(name: String, doc: Document? = null, init: Element.() -> Unit): Element = ownerDocument!!.createElement(name).apply(init)

/**
 * Adds a newly created element which can be configured via a function
 */
@Deprecated("Use Element.appendElement instead.", level = DeprecationLevel.ERROR)
fun Document.addElement(name: String, init: Element.() -> Unit): Element {
    val child = createElement(name).apply(init)
    this.appendChild(child)
    return child
}

@Deprecated("Use Element.appendElement instead", ReplaceWith("appendElement(name, init)", "kotlin.dom.appendElement"), level = DeprecationLevel.ERROR)
fun Element.addElement(name: String, doc: Document? = null, init: Element.() -> Unit): Element = appendElement(name, init)


