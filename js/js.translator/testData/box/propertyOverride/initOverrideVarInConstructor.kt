// EXPECTED_REACHABLE_NODES: 497
// Test for KT-5673

package foo

interface Holder {
    var element: String
}

class BasicHolder : Holder {
    override var element: String = "not ok"

    init {
        element = "ok"
    }
}

fun box(): String {
    assertEquals("ok", BasicHolder().element)

    return "OK"
}