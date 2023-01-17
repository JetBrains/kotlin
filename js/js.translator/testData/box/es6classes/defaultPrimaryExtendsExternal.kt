var sideEffect = ""

open external class E()
abstract class A : E {
    fun print(a: Any) { sideEffect += "#$a" }

    constructor(x: Int, y: Int) : super() {
        print(x + y)
        print(foo())
    }

    constructor(x: Int) : super() {
        print(x)
        print(foo())
    }

    abstract fun foo(): String

    init {
        print("init: " + foo())
    }
}

class O(val x: String) {
    inner class I() : A(1337) {
        override fun foo() = x
    }
}

fun box(): String {
    val o = O("OK")
    val i = o.I()

    assertEquals("#init: OK#1337#OK", sideEffect)

    return i.foo()
}
