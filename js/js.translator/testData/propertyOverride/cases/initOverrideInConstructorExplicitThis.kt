// Test for KT-5673

package foo

trait Holder {
    val element: String
}

class BasicHolder : Holder {
    override val element: String

    {
        this.element = "ok"
    }
}

fun box(): String {
    assertEquals("ok", BasicHolder().element)

    return "OK"
}