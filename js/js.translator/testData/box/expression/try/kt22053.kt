// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1126
package foo

class MyThrowable(message: String?) :  Throwable("through primary: " + message) {
    public var initOrder = ""

    constructor() : this(message = "secondary") {
        initOrder += "2"
    }
    constructor(i: Int) : this() {
        initOrder += "3"
    }

    init { initOrder += "1" }
}

fun box(): String {
    val mt1 = MyThrowable("primary")
    assertEquals(mt1.toString(), "MyThrowable: through primary: primary")
    assertEquals(mt1.initOrder, "1")

    val mt2 = MyThrowable()
    assertEquals(mt2.toString(), "MyThrowable: through primary: secondary")
    assertEquals(mt2.initOrder, "12")

    val mt3 = MyThrowable(1)
    assertEquals(mt3.toString(), "MyThrowable: through primary: secondary")
    assertEquals(mt3.initOrder, "123")

    return "OK"
}
