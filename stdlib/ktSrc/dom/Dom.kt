package std.dom

import std.*
import std.util.*
import java.util.*
import org.w3c.dom.*


// Properties

val Document?.rootElement : Element?
get() = if (this != null) this.getDocumentElement() else null


var Node.text : String
get() {
    if (this is Element) {
        return this.text
    } else {
        return this.getNodeValue() ?: ""
    }
}
set(value) {
    if (this is Element) {
        this.text = value
    } else {
        this.setNodeValue(value)
    }
}

var Element.text : String
get() {
    val buffer = StringBuilder()
    val nodeList = this.getChildNodes()
    if (nodeList != null) {
        var i = 0
        val size = nodeList.getLength()
        while (i < size) {
            val node = nodeList.item(i)
            if (node != null) {
                if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                    buffer.append(node.getNodeValue())
                }
            }
            i++
        }
    }
    return buffer.toString().sure()
}
set(value) {
    // lets remove all the previous text nodes first
    this.setAttribute("id", value)
}

var Element.id : String
get() = this.getAttribute("id")?: ""
set(value) {
    this.setAttribute("id", value)
}

var Element.style : String
get() = this.getAttribute("style")?: ""
set(value) {
    this.setAttribute("style", value)
}

// TODO can we come up with a better name; 'class' is a reserved word?
var Element.cssClass : String
get() = this.getAttribute("class")?: ""
set(value) {
    this.setAttribute("class", value)
}


// Helper methods

/** Searches for elements using the element name, an element ID (if prefixed with dot) or element class (if prefixed with #) */
fun Document?.get(selector: String): List<Element> {
    val root = this?.getDocumentElement()
    return if (root != null) {
        root.get(selector)
    } else {
        Collections.EMPTY_LIST as List<Element>
    }
}

/** Searches for elements using the element name, an element ID (if prefixed with dot) or element class (if prefixed with #) */
fun Element.get(selector: String): List<Element> {
    if (selector.startsWith(".")) {
        // TODO filter by CSS style class
    } else if (selector.startsWith("#")) {
        // TODO lookup by ID
    }
    // TODO assume its a vanilla element name
    return this.getElementsByTagName(selector).toElementList()
}

/** Returns the attribute value or null if its not present */
inline fun Element?.attribute(name: String): String? {
    return this?.getAttribute(name)
}

/** Returns the children of the element as a list */
inline fun Element?.children(): List<Node> {
    return this?.getChildNodes().toList()
}

inline fun Element?.elementsByTagNameNS(namespaceUri: String?, localName: String?): List<Element> {
    return this?.getElementsByTagNameNS(namespaceUri, localName).toElementList()
}

inline fun Document?.elementsByTagNameNS(namespaceUri: String?, localName: String?): List<Element> {
    return this?.getElementsByTagNameNS(namespaceUri, localName).toElementList()
}

val NodeList?.head : Node?
get() = if (this != null && this.getLength() > 0) this.item(0) else null

val NodeList?.first : Node?
get() = this.head

val NodeList?.tail : Node?
get() {
    if (this == null) {
        return null
    } else {
        val s = this.getLength()
        return if (s > 0) this.item(s - 1) else null
    }
}

val NodeList?.last : Node?
get() = this.tail


inline fun NodeList?.toList(): List<Node> {
    return if (this == null) {
        Collections.EMPTY_LIST as List<Node>
    }
    else {
        NodeListAsList(this)
    }
}

inline fun NodeList?.toElementList(): List<Element> {
    return if (this == null) {
        Collections.EMPTY_LIST as List<Element>
    }
    else {
        ElementListAsList(this)
    }
}

fun NodeList?.toXmlString(xmlDeclaration: Boolean = false): String {
    return if (this == null)
        "" else {
        this.toList().toXmlString(xmlDeclaration)
    }
}

class NodeListAsList(val nodeList: NodeList): AbstractList<Node>() {
    override fun get(index: Int): Node {
        val node = nodeList.item(index)
        if (node == null) {
            throw IndexOutOfBoundsException("NodeList does not contain a node at index: " + index)
        } else {
            return node
        }
    }

    override fun size(): Int = nodeList.getLength()
}

class ElementListAsList(val nodeList: NodeList): AbstractList<Element>() {
    override fun get(index: Int): Element {
        val node = nodeList.item(index)
        if (node is Element) {
            return node
        } else {
            if (node == null) {
                throw IndexOutOfBoundsException("NodeList does not contain a node at index: " + index)
            } else {
                throw IllegalArgumentException("Node is not an Element as expected but is $node")
            }
        }
    }

    override fun size(): Int = nodeList.getLength()

}
// Syntax sugar

inline fun Node.plus(child: Node?): Node {
    if (child != null) {
        this.appendChild(child)
    }
    return this
}

inline fun Element.plus(text: String?): Element = this.addText(text)

inline fun Element.plusAssign(text: String?): Element = this.addText(text)


// Builder

/*
Creates a new element which can be configured via a function
*/
fun Document.createElement(name: String, init: Element.()-> Unit): Element {
    val elem = this.createElement(name).sure()
    elem.init()
    return elem
}

/*
Creates a new element to an element which has an owner Document which can be configured via a function
*/
fun Element.createElement(name: String, doc: Document? = null, init: Element.()-> Unit): Element {
    val elem = ownerDocument(doc).createElement(name).sure()
    elem.init()
    return elem
}

/*
Returns the owner document of the element or uses the provided document
*/
fun Node.ownerDocument(doc: Document? = null): Document {
    val answer = if (this is Document) this as Document
    else if (doc == null) this.getOwnerDocument()
    else doc

    if (answer == null) {
        throw IllegalArgumentException("Element does not have an ownerDocument and none was provided for: ${this}")
    } else {
        return answer
    }
}

/*
Adds a newly created element which can be configured via a function
*/
fun Document.addElement(name: String, init: Element.()-> Unit): Element {
    val child = createElement(name, init)
    this.appendChild(child)
    return child
}

/*
Adds a newly created element to an element which has an owner Document which can be configured via a function
*/
fun Element.addElement(name: String, doc: Document? = null, init: Element.()-> Unit): Element {
    val child = createElement(name, doc, init)
    this.appendChild(child)
    return child
}

/*
Adds a newly created text node to an element which either already has an owner Document or one must be provided as a parameter
*/
fun Element.addText(text: String?, doc: Document? = null): Element {
    if (text != null) {
        val child = ownerDocument(doc).createTextNode(text)
        this.appendChild(child)
    }
    return this
}
