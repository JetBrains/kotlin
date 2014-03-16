package test.browser

import js.dom.html.document
import org.w3c.dom.Node

fun box(): String {
    val element = document.getElementById("foo")!!
    val textNode = document.createTextNode("Some Dynamically Created Content!!!")
    element.appendChild(textNode)
    if (textNode.nodeType != Node.TEXT_NODE) return "The type of the node is ${textNode.nodeType}, ${Node.TEXT_NODE} was expected"
    if (element.nodeType != Node.ELEMENT_NODE) return "The type of the node is ${element.nodeType}, ${Node.ELEMENT_NODE} was expected"
    return element.textContent
}