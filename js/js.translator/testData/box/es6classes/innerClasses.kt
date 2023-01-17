// EXPECTED_REACHABLE_NODES: 1343

abstract class A {
    abstract fun foo(): String

    val ss = foo() + "K"
}

class O(val s: String) {
    inner class I() : A() {
        override fun foo() = s
    }

    fun result() = I().ss
}

fun box(): String {
    val o = O("O")
    return o.result()
}
