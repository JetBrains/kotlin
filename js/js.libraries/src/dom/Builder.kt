package kotlin.dom.build

import org.w3c.dom.*
import kotlin.dom.*

/**
 * Creates a new element which can be configured via a function
 */
fun Document.createElement(name: String, init: Element.() -> Unit): Element {
    val elem = this.createElement(name)!!
    elem.init()
    return elem
}

/**
 * Creates a new element to an element which has an owner Document which can be configured via a function
 */
fun Element.createElement(name: String, doc: Document? = null, init: Element.() -> Unit): Element {
    val elem = ownerDocument(doc).createElement(name)!!
    elem.init()
    return elem
}

/**
 * Adds a newly created element which can be configured via a function
 */
fun Document.addElement(name: String, init: Element.() -> Unit): Element {
    val child = createElement(name, init)
    this.appendChild(child)
    return child
}

/**
 * Adds a newly created element to an element which has an owner Document which can be configured via a function
 */
fun Element.addElement(name: String, doc: Document? = null, init: Element.() -> Unit): Element {
    val child = createElement(name, doc, init)
    this.appendChild(child)
    return child
}

