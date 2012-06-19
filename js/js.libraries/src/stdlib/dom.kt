package kotlin.dom

import org.w3c.dom.Document
import org.w3c.dom.Node

fun createDocument(): Document {
    return browser.document.implementation.createDocument(null, null, null)
}

native public val Node.outerHTML: String
get() = js.noImpl

/** Converts the node to an XML String */
public fun Node.toXmlString(xmlDeclaration: Boolean = false): String {
    return this.outerHTML
}
