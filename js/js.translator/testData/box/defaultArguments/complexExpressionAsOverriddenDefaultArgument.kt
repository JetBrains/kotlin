// EXPECTED_REACHABLE_NODES: 509
// http://youtrack.jetbrains.com/issue/KT-4879
// JS: extra side effect when use when in default arguments

package foo

var global: String = ""

fun bar(): Int {
    global += ":bar:"
    return 100
}

fun baz() = 1

open class A {
    open fun foo(a: Int = when (baz()) { 1 -> bar(); else -> 0 }): Int = a + 1

    open fun bar0(x: String = try { global } finally {}): String {
        return "bar: ${x}"
    }
}

class B : A() {
    override fun foo(a: Int) = a + 2

    override fun bar0(x: String) = "B.bar: $x"
}

fun box(): String {
    global = ""
    val b = B()
    assertEquals(102, b.foo(100))
    assertEquals("", global)

    assertEquals(102, b.foo())
    assertEquals(":bar:", global)

    assertEquals("B.bar: q", b.bar0("q"))
    assertEquals("B.bar: :bar:", b.bar0())

    return "OK"
}