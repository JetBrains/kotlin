package sample

import kotlin.browser.document

fun myApp() {
    val element = document.getElementById("foo")
    if (element != null) {
        element.appendChild(document.createTextNode("Some Dynamically Created Content!!!"))
    }
}
