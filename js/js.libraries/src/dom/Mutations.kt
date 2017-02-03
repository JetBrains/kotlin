package kotlin.dom

import org.w3c.dom.*

/** Removes all the children from this node */
fun Node.clear() {
    while (hasChildNodes()) {
        removeChild(firstChild!!)
    }
}

/**
 * Removes this node from parent node. Does nothing if no parent node
 */
fun Node.removeFromParent() {
    parentNode?.removeChild(this)
}

operator fun Node.plus(child: Node): Node {
    appendChild(child)
    return this
}

operator fun Element.plus(text: String): Element = appendText(text)
operator fun Element.plusAssign(text: String): Unit {
    appendText(text)
}

/** Returns the owner document of the element or uses the provided document */
fun Node.ownerDocument(doc: Document? = null): Document = when {
    nodeType == Node.DOCUMENT_NODE -> this as Document
    else -> doc ?: ownerDocument ?: throw IllegalArgumentException("Neither node contains nor parameter doc provides an owner document for $this")
}


/**
 * Creates text node and append it to the element
 */
fun Element.appendText(text: String, doc: Document? = null): Element {
    appendChild(ownerDocument(doc).createTextNode(text))
    return this
}

/**
 * Appends the node to the specified parent element
 */
fun Node.appendTo(parent: Element) {
    parent.appendChild(this)
}
