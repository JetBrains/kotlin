// EXPECTED_REACHABLE_NODES: 495
// http://youtrack.jetbrains.com/issue/KT-4879
// JS: extra side effect when use when in default arguments

package foo

var global: String = ""

fun bar(): Int {
    global += ":bar:"
    return 100
}

fun baz() = 1

fun foo(a: Int = when (baz()) { 1 -> bar(); else -> 0 }): Int = a + 1

fun bar0(x: String = try { global } finally {}): String {
    return "bar: ${x}"
}

fun box(): String {
    global = ""
    assertEquals(101, foo(100))
    assertEquals("", global)

    assertEquals(101, foo())
    assertEquals(":bar:", global)

    return "OK"
}