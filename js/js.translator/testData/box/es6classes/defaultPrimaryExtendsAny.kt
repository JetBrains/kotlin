var sideEffect = ""

abstract class A {
    fun print(a: Any) { sideEffect += "#$a" }

    constructor(x: Int, y: Int) {
        print(x + y)
        print(foo())
    }

    abstract fun foo(): String

    init {
        print("init: " + foo())
    }
}

class O(val x: String) {
    inner class I() : A(13, 37) {
        override fun foo() = x
    }
}

fun box(): String {
    val o = O("OK")
    val i = o.I()

    assertEquals("#init: OK#50#OK", sideEffect)

    return i.foo()
}
