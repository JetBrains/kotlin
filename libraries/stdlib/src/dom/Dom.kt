package kotlin.dom

import kotlin.*
import kotlin.support.*
import kotlin.util.*
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
    this.setIdAttribute("id", true)
}

var Element.style : String
get() = this.getAttribute("style")?: ""
set(value) {
    this.setAttribute("style", value)
}

var Element.classes : String
get() = this.getAttribute("class")?: ""
set(value) {
    this.setAttribute("class", value)
}

var Element.classSet : Set<String>
get() {
    val answer = LinkedHashSet<String>()
    val array = this.classes.split("""\s""")
    for (s in array) {
        if (s != null && s.size > 0) {
            answer.add(s)
        }
    }
    return answer
}
set(value) {
    this.classes = value.join(" ")
}


// Helper methods

/** Returns true if the element has the given CSS class style in its 'class' attribute */
fun Element.hasClass(cssClass: String): Boolean {
    val c = this.classes
    return if (c != null)
        c.matches("""(^|.*\s+)$cssClass($|\s+.*)""")
    else false
}

/** Adds the given CSS class to this element's 'class' attribute */
fun Element.addClass(cssClass: String): Boolean {
    val classSet = this.classSet
    val answer = classSet.add(cssClass)
    if (answer) {
        this.classSet = classSet
    }
    return answer
}

/** Removes the given CSS class to this element's 'class' attribute */
fun Element.removeClass(cssClass: String): Boolean {
    val classSet = this.classSet
    val answer = classSet.remove(cssClass)
    if (answer) {
        this.classSet = classSet
    }
    return answer
}

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

/** Searches for elements using the element name, an element ID (if prefixed with dot) or element class (if prefixed with #) */
fun Document?.get(selector: String): List<Element> {
    val root = this?.getDocumentElement()
    return if (root != null) {
        if (selector == "*") {
            elements
        } else if (selector.startsWith(".")) {
            elements.filter{ it.hasClass(selector.substring(1)) }.toList()
        } else if (selector.startsWith("#")) {
            val id = selector.substring(1)
            val element = this?.getElementById(id)
            return if (element != null)
                Collections.singletonList(element).sure() as List<Element>
            else
                Collections.EMPTY_LIST.sure() as List<Element>
        } else {
            //  assume its a vanilla element name
            elements(selector)
        }
    } else {
        Collections.EMPTY_LIST as List<Element>
    }
}

/** Searches for elements using the element name, an element ID (if prefixed with dot) or element class (if prefixed with #) */
fun Element.get(selector: String): List<Element> {
    return if (selector == "*") {
        elements
    } else if (selector.startsWith(".")) {
        elements.filter{ it.hasClass(selector.substring(1)) }.toList()
    } else if (selector.startsWith("#")) {
        val element = this.getOwnerDocument()?.getElementById(selector.substring(1))
        return if (element != null)
            Collections.singletonList(element).sure() as List<Element>
        else
            Collections.EMPTY_LIST.sure() as List<Element>
    } else {
        //  assume its a vanilla element name
        elements(selector)
    }
}

/** Returns an [[Iterator]] over the next siblings of this node */
fun Node.nextSiblings() : Iterator<Node> = NextSiblingIterator(this)

protected class NextSiblingIterator(var node: Node) : AbstractIterator<Node>() {

    override fun computeNext(): Node? {
        val next = node.getNextSibling()
        if (next != null) {
            node = next
            return next
        } else {
            done()
            return null
        }
    }
}
/** Returns an [[Iterator]] over the next siblings of this node */
fun Node.previousSiblings() : Iterator<Node> = PreviousSiblingIterator(this)

protected class PreviousSiblingIterator(var node: Node) : AbstractIterator<Node>() {

    override fun computeNext(): Node? {
        val next = node.getPreviousSibling()
        if (next != null) {
            node = next
            return next
        } else {
            done()
            return null
        }
    }
}

/** Returns an [[Iterator]] of all the next [[Element]] siblings */
fun Node.nextElements(): Iterator<Element> = nextSiblings().filterIs<Node, Element>()

/** Returns an [[Iterator]] of all the previous [[Element]] siblings */
fun Node.previousElements(): Iterator<Element> = previousSiblings().filterIs<Node, Element>()

/** Returns the attribute value or empty string if its not present */
inline fun Element.attribute(name: String): String {
    return this.getAttribute(name) ?: ""
}

/** Returns the children of the element as a list */
inline fun Element?.children(): List<Node> {
    return this?.getChildNodes().toList()
}

/** The child elements of this document */
val Document?.elements : List<Element>
get() = this?.getElementsByTagName("*").toElementList()

/** The child elements of this elements */
val Element?.elements : List<Element>
get() = this?.getElementsByTagName("*").toElementList()


/** Returns all the child elements given the local element name */
inline fun Element?.elements(localName: String?): List<Element> {
    return this?.getElementsByTagName(localName).toElementList()
}

/** Returns all the elements given the local element name */
inline fun Document?.elements(localName: String?): List<Element> {
    return this?.getElementsByTagName(localName).toElementList()
}

/** Returns all the child elements given the namespace URI and local element name */
inline fun Element?.elements(namespaceUri: String?, localName: String?): List<Element> {
    return this?.getElementsByTagNameNS(namespaceUri, localName).toElementList()
}

/** Returns all the elements given the namespace URI and local element name */
inline fun Document?.elements(namespaceUri: String?, localName: String?): List<Element> {
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

/** Converts the node list to an XML String */
fun NodeList?.toXmlString(xmlDeclaration: Boolean = false): String {
    return if (this == null)
        "" else {
        nodesToXmlString(this.toList(), xmlDeclaration)
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

/**
 * Creates a new element which can be configured via a function
 */
fun Document.createElement(name: String, init: Element.()-> Unit): Element {
    val elem = this.createElement(name).sure()
    elem.init()
    return elem
}

/**
 * Creates a new element to an element which has an owner Document which can be configured via a function
 */
fun Element.createElement(name: String, doc: Document? = null, init: Element.()-> Unit): Element {
    val elem = ownerDocument(doc).createElement(name).sure()
    elem.init()
    return elem
}

/** Returns the owner document of the element or uses the provided document */
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

/**
Adds a newly created element which can be configured via a function
*/
fun Document.addElement(name: String, init: Element.()-> Unit): Element {
    val child = createElement(name, init)
    this.appendChild(child)
    return child
}

/**
Adds a newly created element to an element which has an owner Document which can be configured via a function
*/
fun Element.addElement(name: String, doc: Document? = null, init: Element.()-> Unit): Element {
    val child = createElement(name, doc, init)
    this.appendChild(child)
    return child
}

/**
Adds a newly created text node to an element which either already has an owner Document or one must be provided as a parameter
*/
fun Element.addText(text: String?, doc: Document? = null): Element {
    if (text != null) {
        val child = ownerDocument(doc).createTextNode(text)
        this.appendChild(child)
    }
    return this
}
