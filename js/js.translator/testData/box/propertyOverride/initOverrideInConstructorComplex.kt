// EXPECTED_REACHABLE_NODES: 506
// Test for KT-5673

package foo

interface Holder {
    val element: String
}

open class BasicHolder : Holder {
    override val element: String
        get() = field + field

    init {
        element = "1"
    }
}

class AdvancedHolder : BasicHolder() {
    override val element: String

    init {
        element = super.element + super.element
    }
}

fun box(): String {
    assertEquals("1111", AdvancedHolder().element)

    return "OK"
}