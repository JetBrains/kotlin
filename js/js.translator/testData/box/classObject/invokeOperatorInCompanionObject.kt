// EXPECTED_REACHABLE_NODES: 500
// See KT-11100
package foo

class Div {
    var className: String? = null

    companion object {
        operator fun invoke(init: Div.() -> Unit): Div {
            val div = Div()
            div.init()
            return div
        }
    }
}

fun box(): String {
    val x = Div {
        className = "ui container"
    }
    assertEquals("ui container", x.className)
    return "OK"
}