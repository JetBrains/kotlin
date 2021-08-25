package testProject

import kotlinx.browser.document

class DomPrinter(val id: String) {
    val element by lazy { document.getElementById(id) }

    fun print(msg: String) {
        element?.innerHTML = msg
    }
}

actual typealias Printer = DomPrinter

@JsName("handler")
fun handler(id: String): (a: String, b: String) -> Unit {
    val printer = DomPrinter(id)
    return { a, b -> businessLogic(a, b, printer) }
}