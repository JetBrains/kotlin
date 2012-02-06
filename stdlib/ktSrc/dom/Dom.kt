package std.dom

import org.w3c.dom.*
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import std.getOrElse

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