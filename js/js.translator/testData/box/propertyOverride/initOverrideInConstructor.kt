// EXPECTED_REACHABLE_NODES: 497
// Test for KT-5673

package foo

interface Holder {
    val element: String
}

class BasicHolder : Holder {
    override val element: String

    init {
        element = "ok"
    }
}

fun box(): String {
    assertEquals("ok", BasicHolder().element)

    return "OK"
}