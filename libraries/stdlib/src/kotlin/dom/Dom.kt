package kotlin.dom

import kotlin.support.*
import java.util.*
import org.w3c.dom.*

// TODO should not need this - its here for the JS stuff
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException

// Properties

private fun emptyElementList(): List<Element> = Collections.emptyList<Element>()
private fun emptyNodeList(): List<Node> = Collections.emptyList<Node>()

public var Node.text: String
    get() {
        return textContent
    }
    set(value) {
        textContent = value
    }

public var Element.childrenText: String
    get() {
        val buffer = StringBuilder()
        val nodeList = this.childNodes
        var i = 0
        val size = nodeList.length
        while (i < size) {
            val node = nodeList.item(i)
            if (node != null) {
                if (node.isText()) {
                    buffer.append(node.nodeValue)
                }
            }
            i++
        }
        return buffer.toString()
    }
    set(value) {
        val element = this
        // lets remove all the previous text nodes first
        for (node in element.children()) {
            if (node.isText()) {
                removeChild(node)
            }
        }
        element.addText(value)
    }

public var Element.id: String
    get() = this.getAttribute("id") ?: ""
    set(value) {
        this.setAttribute("id", value)
        this.setIdAttribute("id", true)
    }

public var Element.style: String
    get() = this.getAttribute("style") ?: ""
    set(value) {
        this.setAttribute("style", value)
    }

public var Element.classes: String
    get() = this.getAttribute("class") ?: ""
    set(value) {
        this.setAttribute("class", value)
    }

/** Returns true if the element has the given CSS class style in its 'class' attribute */
public fun Element.hasClass(cssClass: String): Boolean {
    val c = this.classes
    return c.matches("""(^|.*\s+)$cssClass($|\s+.*)""")
}


/** Returns the children of the element as a list */
public fun Element?.children(): List<Node> {
    return this?.childNodes.toList()
}

/** Returns the child elements of this element */
public fun Element?.childElements(): List<Element> {
    return children().filter<Node>{ it.nodeType == Node.ELEMENT_NODE }.map { it as Element }
}

/** Returns the child elements of this element with the given name */
public fun Element?.childElements(name: String): List<Element> {
    return children().filter<Node>{ it.nodeType == Node.ELEMENT_NODE && it.nodeName == name }.map { it as Element }
}

/** The descendent elements of this document */
public val Document?.elements: List<Element>
    get() = this?.getElementsByTagName("*").toElementList()

/** The descendant elements of this elements */
public val Element?.elements: List<Element>
    get() = this?.getElementsByTagName("*").toElementList()


/** Returns all the descendant elements given the local element name */
public fun Element?.elements(localName: String): List<Element> {
    return this?.getElementsByTagName(localName).toElementList()
}

/** Returns all the descendant elements given the local element name */
public fun Document?.elements(localName: String): List<Element> {
    return this?.getElementsByTagName(localName).toElementList()
}

/** Returns all the descendant elements given the namespace URI and local element name */
public fun Element?.elements(namespaceUri: String, localName: String): List<Element> {
    return this?.getElementsByTagNameNS(namespaceUri, localName).toElementList()
}

/** Returns all the descendant elements given the namespace URI and local element name */
public fun Document?.elements(namespaceUri: String, localName: String): List<Element> {
    return this?.getElementsByTagNameNS(namespaceUri, localName).toElementList()
}

public fun NodeList?.toList(): List<Node> {
    return if (this == null) {
        // TODO the following is easier to convert to JS
        emptyNodeList()
    }
    else {
        NodeListAsList(this)
    }
}

public fun NodeList?.toElementList(): List<Element> {
    return if (this == null) {
        // TODO the following is easier to convert to JS
        //emptyElementList()
        ArrayList<Element>()
    }
    else {
        ElementListAsList(this)
    }
}

/** Searches for elements using the element name, an element ID (if prefixed with dot) or element class (if prefixed with #) */
public fun Document?.get(selector: String): List<Element> {
    val root = this?.documentElement
    return if (root != null) {
        if (selector == "*") {
            elements
        } else if (selector.startsWith(".")) {
            elements.filter{ it.hasClass(selector.substring(1)) }.toList()
        } else if (selector.startsWith("#")) {
            val id = selector.substring(1)
            val element = this?.getElementById(id)
            return if (element != null)
                arrayListOf(element)
            else
                emptyElementList()
        } else {
            //  assume its a vanilla element name
            elements(selector)
        }
    } else {
        emptyElementList()
    }
}

/** Searches for elements using the element name, an element ID (if prefixed with dot) or element class (if prefixed with #) */
public fun Element.get(selector: String): List<Element> {
    return if (selector == "*") {
        elements
    } else if (selector.startsWith(".")) {
        elements.filter{ it.hasClass(selector.substring(1)) }.toList()
    } else if (selector.startsWith("#")) {
        val element = this.ownerDocument?.getElementById(selector.substring(1))
        return if (element != null)
            arrayListOf(element)
        else
            emptyElementList()
    } else {
        //  assume its a vanilla element name
        elements(selector)
    }
}


// Helper methods

/** TODO this approach generates compiler errors...

fun Element.addClass(varargs cssClasses: Array<String>): Boolean {
    val set = this.classSet
    var answer = false
    for (cs in cssClasses) {
        if (set.add(cs)) {
            answer = true
        }
    }
    if (answer) {
        this.classSet = classSet
    }
    return answer
}

fun Element.removeClass(varargs cssClasses: Array<String>): Boolean {
    val set = this.classSet
    var answer = false
    for (cs in cssClasses) {
        if (set.remove(cs)) {
            answer = true
        }
    }
    if (answer) {
        this.classSet = classSet
    }
    return answer
}
*/

private class NodeListAsList(private val nodeList: NodeList) : AbstractList<Node>() {
    override fun get(index: Int): Node {
        val node = nodeList.item(index)
        if (node == null) {
            throw IndexOutOfBoundsException("NodeList does not contain a node at index: " + index)
        } else {
            return node
        }
    }

    override fun size(): Int = nodeList.length
}

private class ElementListAsList(private val nodeList: NodeList) : AbstractList<Element>() {
    override fun get(index: Int): Element {
        val node = nodeList.item(index)
        if (node == null) {
            throw IndexOutOfBoundsException("NodeList does not contain a node at index: " + index)
        } else if (node.nodeType == Node.ELEMENT_NODE) {
            return node as Element
        } else {
            throw IllegalArgumentException("Node is not an Element as expected but is $node")
        }
    }

    override fun size(): Int = nodeList.length

}

/** Removes all the children from this node */
public fun Node.clear(): Unit {
    while (true) {
        val child = firstChild
        if (child == null) {
            return
        } else {
            removeChild(child)
        }
    }
}

/** Returns an [[Iterator]] over the next siblings of this node */
public fun Node.nextSiblings(): Iterable<Node> = NextSiblings(this)

private class NextSiblings(private var node: Node) : Iterable<Node> {
    override fun iterator(): Iterator<Node> = object : AbstractIterator<Node>() {
        override fun computeNext(): Unit {
            val nextValue = node.nextSibling
            if (nextValue != null) {
                setNext(nextValue)
                node = nextValue
            } else {
                done()
            }
        }
    }
}

/** Returns an [[Iterator]] over the next siblings of this node */
public fun Node.previousSiblings(): Iterable<Node> = PreviousSiblings(this)

private class PreviousSiblings(private var node: Node) : Iterable<Node> {
    override fun iterator(): Iterator<Node> = object : AbstractIterator<Node>() {
        override fun computeNext(): Unit {
            val nextValue = node.previousSibling
            if (nextValue != null) {
                setNext(nextValue)
                node = nextValue
            } else {
                done()
            }
        }
    }
}

/** Returns true if this node is a Text node or a CDATA node */
public fun Node.isText(): Boolean {
    val nt = nodeType
    return nt == Node.TEXT_NODE || nt == Node.CDATA_SECTION_NODE
}

/** Returns the attribute value or empty string if its not present */
public fun Element.attribute(name: String): String {
    return this.getAttribute(name) ?: ""
}

public val NodeList?.head: Node?
    get() = if (this != null && this.length > 0) this.item(0) else null

public val NodeList?.first: Node?
    get() = this.head

public val NodeList?.tail: Node?
    get() {
        if (this == null) {
            return null
        }
        else {
            val s = this.length
            return if (s > 0) this.item(s - 1) else null
        }
    }

public val NodeList?.last: Node?
    get() = this.tail


/** Converts the node list to an XML String */
public fun NodeList?.toXmlString(xmlDeclaration: Boolean = false): String {
    return if (this == null)
        "" else {
        nodesToXmlString(this.toList(), xmlDeclaration)
    }
}

/** Converts the collection of nodes to an XML String */
public fun nodesToXmlString(nodes: Iterable<Node>, xmlDeclaration: Boolean = false): String {
    // TODO this should work...
    // return this.map{it.toXmlString()}.join("")
    val builder = StringBuilder()
    for (n in nodes) {
        builder.append(n.toXmlString(xmlDeclaration))
    }
    return builder.toString()
}

// Syntax sugar

public fun Node.plus(child: Node?): Node {
    if (child != null) {
        this.appendChild(child)
    }
    return this
}

public fun Element.plus(text: String?): Element = this.addText(text)

public fun Element.plusAssign(text: String?): Element = this.addText(text)


// Builder

/**
 * Creates a new element which can be configured via a function
 */
public fun Document.createElement(name: String, init: Element.() -> Unit): Element {
    val elem = this.createElement(name)!!
    elem.init()
    return elem
}

/**
 * Creates a new element to an element which has an owner Document which can be configured via a function
 */
public fun Element.createElement(name: String, doc: Document? = null, init: Element.() -> Unit): Element {
    val elem = ownerDocument(doc).createElement(name)!!
    elem.init()
    return elem
}

/** Returns the owner document of the element or uses the provided document */
public fun Node.ownerDocument(doc: Document? = null): Document {
    val answer = if (this.nodeType == Node.DOCUMENT_NODE) this as Document
    else if (doc == null) this.ownerDocument
    else doc

    if (answer == null) {
        throw IllegalArgumentException("Element does not have an ownerDocument and none was provided for: ${this}")
    } else {
        return answer
    }
}

/**
Adds a newly created element which can be configured via a function
*/
public fun Document.addElement(name: String, init: Element.() -> Unit): Element {
    val child = createElement(name, init)
    this.appendChild(child)
    return child
}

/**
Adds a newly created element to an element which has an owner Document which can be configured via a function
*/
public fun Element.addElement(name: String, doc: Document? = null, init: Element.() -> Unit): Element {
    val child = createElement(name, doc, init)
    this.appendChild(child)
    return child
}

/**
Adds a newly created text node to an element which either already has an owner Document or one must be provided as a parameter
*/
public fun Element.addText(text: String?, doc: Document? = null): Element {
    if (text != null) {
        val child = this.ownerDocument(doc).createTextNode(text)!!
        this.appendChild(child)
    }
    return this
}
