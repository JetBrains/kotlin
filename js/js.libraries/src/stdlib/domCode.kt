package kotlin.dom

import org.w3c.dom.Document
import org.w3c.dom.Node

deprecated("use declarations from org.w3c.dom instead: create document directly via constructor Document() or use document.implementation.createDocument")
public fun createDocument(): Document = Document()

deprecated("use member property outerHTML of Element class instead")
native public val Node.outerHTML: String get() = noImpl

/** Converts the node to an XML String */
deprecated("use outerHTML directly")
public fun Node.toXmlString(): String = this.outerHTML

/** Converts the node to an XML String */
deprecated("use outerHTML directly")
public fun Node.toXmlString(xmlDeclaration: Boolean): String = this.outerHTML
