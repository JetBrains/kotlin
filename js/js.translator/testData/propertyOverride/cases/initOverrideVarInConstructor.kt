// Test for KT-5673

package foo

trait Holder {
    var element: String
}

class BasicHolder : Holder {
    override var element: String = "not ok"

    {
        element = "ok"
    }
}

fun box(): String {
    assertEquals("ok", BasicHolder().element)

    return "OK"
}