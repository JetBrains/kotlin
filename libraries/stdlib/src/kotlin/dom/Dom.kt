package kotlin.dom

import kotlin.support.*
import java.util.*
import org.w3c.dom.*

// TODO should not need this - its here for the JS stuff
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException

// Properties

deprecated("use textContent directly instead")
public var Node.text: String
    get() = textContent ?: ""
    set(value) {
        textContent = value
    }

deprecated("You shouldn't use it as setter will drop all elements and get may return not exactly content user can expect")
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

public var Element.classes: String
    get() = this.getAttribute("class") ?: ""
    set(value) {
        this.setAttribute("class", value)
    }

/** Returns true if the element has the given CSS class style in its 'class' attribute */
public fun Element.hasClass(cssClass: String): Boolean = classes.matches("""(^|.*\s+)$cssClass($|\s+.*)""")


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

public fun NodeList?.asList() : List<Node> = if (this == null) emptyList() else NodeListAsList(this)
deprecated("use asList instead")
public fun NodeList?.toList(): List<Node> = asList()

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
                emptyList()
        } else {
            //  assume its a vanilla element name
            elements(selector)
        }
    } else {
        emptyList()
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
            emptyList()
    } else {
        //  assume its a vanilla element name
        elements(selector)
    }
}


// Helper methods


/**
 * Adds CSS class to element. Has no effect if all specified classes are already in class attribute of the element
 */
public fun Element.addClass(vararg cssClasses: String): Boolean {
    val missingClasses = cssClasses.filterNot { hasClass(it) }
    if (missingClasses.isNotEmpty()) {
        val presentClasses = classes.trim()
        classes = StringBuilder {
            append(presentClasses)
            if (!presentClasses.isEmpty()) {
                append(" ")
            }
            missingClasses.joinTo(this, " ")
        }.toString()
        return true
    }

    return false
}

/**
 * Removes all [cssClasses] from element. Has no effect if all specified classes are missing in class attribute of the element
 */
public fun Element.removeClass(vararg cssClasses: String): Boolean {
    if (cssClasses.any { hasClass(it) }) {
        val toBeRemoved = cssClasses.toSet()
        classes = classes.trim().split("\\s+".toRegex()).filter { it !in toBeRemoved }.joinToString(" ")
        return true
    }

    return false
}

/** Removes all the children from this node */
public fun Node.clear() {
    while (hasChildNodes()) {
        removeChild(firstChild!!)
    }
}

/**
 * Removes this node from parent node. Does nothing if no parent node
 */
public fun Node.removeFromParent() {
    parentNode?.removeChild(this)
}

private class NodeListAsList(private val delegate: NodeList) : AbstractList<Node>() {
    override fun size(): Int = delegate.length

    override fun get(index: Int): Node = when {
        index in 0..size() - 1 -> delegate.item(index)!!
        else -> throw IndexOutOfBoundsException("index $index is not in range [0 .. ${size() - 1})")
    }
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
deprecated("use property isText instead")
public fun Node.isText() : Boolean = nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE

/**
 * it is *true* when [Node.nodeType] is TEXT_NODE or CDATA_SECTION_NODE
 */
public val Node.isText : Boolean
    get() = nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE


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
    return nodes.map { it.toXmlString(xmlDeclaration) }.join()
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

/**
 * Creates text node and append it to the element
 */
public fun Element.appendText(text: String, doc : Document = this.ownerDocument!!) {
    appendChild(doc.createTextNode(text))
}

/**
 * Appends the node to the specified parent element
 */
public fun Node.appendTo(parent: Element) {
    parent.appendChild(this)
}
