package kotlin.dom

import org.w3c.dom.Document
import org.w3c.dom.Node

deprecated("use org.w3c.dom instead: create document directly via constructor Document() or use document.implementation.createDocument")
public fun createDocument(): Document {
    return kotlin.js.dom.html.document.implementation.createDocument(null, null, null)
}

deprecated("use org.w3c.dom instead")
native public val Node.outerHTML: String get() = noImpl

/** Converts the node to an XML String */
deprecated("use outerHTML directly")
public fun Node.toXmlString(): String = this.outerHTML

/** Converts the node to an XML String */
deprecated("use outerHTML directly")
public fun Node.toXmlString(xmlDeclaration: Boolean): String = this.outerHTML
