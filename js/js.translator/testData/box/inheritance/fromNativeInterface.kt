// EXPECTED_REACHABLE_NODES: 491
package foo

external interface A {
    val bar: Int? get() = definedExternally
    fun foo(): String
}

class C : A {
    override fun foo() = "foo"
}

fun box(): String {
    val c = C()

    return "OK"
}
