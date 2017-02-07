package kotlin.dom

import org.w3c.dom.*

/** Removes all the children from this node. */
public fun Node.clear() {
    while (hasChildNodes()) {
        removeChild(firstChild!!)
    }
}

/**
 * Removes this node from parent node. Does nothing if no parent node
 */
@Deprecated("Use parentNode?.removeChild(this) instead.", ReplaceWith("this.also { it.parentNode?.removeChild(it) }"), level = DeprecationLevel.ERROR)
public fun Node.removeFromParent() {
    parentNode?.removeChild(this)
}

@Deprecated("Use appendChild instead", ReplaceWith("appendChild(node)"), level = DeprecationLevel.ERROR)
operator fun Node.plus(child: Node): Node {
    appendChild(child)
    return this
}

@Deprecated("Use appendText instead", ReplaceWith("appendText(text)"), level = DeprecationLevel.ERROR)
operator fun Element.plus(text: String): Element = appendText(text)

@Deprecated("Use appendText instead", ReplaceWith("appendText(text)"), level = DeprecationLevel.ERROR)
operator fun Element.plusAssign(text: String): Unit {
    appendText(text)
}

/** Returns the owner document of the element or uses the provided document */
@Deprecated("Use ownerDocument property instead", ReplaceWith("this.ownerDocument ?: doc ?: error(\"no ownerDocument\")"), level = DeprecationLevel.ERROR)
fun Node.ownerDocument(doc: Document? = null): Document = when {
    nodeType == Node.DOCUMENT_NODE -> this as Document
    else -> doc ?: ownerDocument ?: throw IllegalArgumentException("Neither node contains nor parameter doc provides an owner document for $this")
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

/**
 * Appends the node to the specified parent element
 */
@Deprecated("Use parent.appendChild instead", ReplaceWith("this.let(parent::appendChild)"), level = DeprecationLevel.ERROR)
fun Node.appendTo(parent: Element) {
    parent.appendChild(this)
}
