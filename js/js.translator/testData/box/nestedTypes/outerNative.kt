// EXPECTED_REACHABLE_NODES: 488
package foo

external class A(x: Int) {
    var x: Int
        get() = definedExternally
        set(value) = definedExternally

    fun foo(): Int = definedExternally

    class B(value: Int) {
        val value: Int

        fun bar(): Int = definedExternally
    }
}

fun box(): String {
    var b = A.B(23)
    if (b.bar() != 10023) return "failed1: ${b.bar()}"

    return "OK"
}

