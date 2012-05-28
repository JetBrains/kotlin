package test.browser

import kotlin.browser.document

fun foo(): String {
    val element = document.getElementById("foo")
    if (element != null) {
        element.appendChild(document.createTextNode("Some Dynamically Created Content!!!"))
    }
    return element.getTextContent()
}
