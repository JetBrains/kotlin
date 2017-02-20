package kotlin.dom

import org.w3c.dom.*

/** Removes all the children from this node. */
public fun Node.clear() {
    while (hasChildNodes()) {
        removeChild(firstChild!!)
    }
}

/**
 * Creates text node and append it to the element.
 *
 * @returns this element
 */
fun Element.appendText(text: String): Element {
    appendChild(ownerDocument!!.createTextNode(text))
    return this
}
