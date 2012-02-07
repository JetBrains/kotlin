package std.dom

import std.*
import org.w3c.dom.*


// Properties

var Element.id : String
get() = this.getAttribute("id").getOrElse("")
set(value) {
    this.setAttribute("id", value)
}

var Element.style : String
get() = this.getAttribute("style").getOrElse("")
set(value) {
    this.setAttribute("style", value)
}

// TODO can we come up with a better name; 'class' is a reserved word?
var Element.cssClass : String
get() = this.getAttribute("class").getOrElse("")
set(value) {
    this.setAttribute("class", value)
}

// Syntax sugar

inline fun Node.plus(child: Node?): Node {
    if (child != null) {
        this.appendChild(child)
    }
    return this
}


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
fun Element.ownerDocument(doc: Document? = null): Document {
    val answer = if (doc == null) this.getOwnerDocument() else doc
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
fun Element.addText(text: String, doc: Document? = null): Element {
    val child = ownerDocument(doc).createTextNode(text)
    this.appendChild(child)
    return this
}
