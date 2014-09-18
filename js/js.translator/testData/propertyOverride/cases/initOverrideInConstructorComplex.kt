// Test for KT-5673

package foo

trait Holder {
    val element: String
}

open class BasicHolder : Holder {
    override val element: String
        get() = $element + $element

    {
        element = "1"
    }
}

class AdvancedHolder : BasicHolder() {
    override val element: String

    {
        element = super.element + super.element
    }
}

fun box(): String {
    assertEquals("1111", AdvancedHolder().element)

    return "OK"
}