// EXPECTED_REACHABLE_NODES: 496
package foo

var global: String = ""

fun bar(): Int {
    global += ":bar:"
    return 100
}

fun baz() = 1

class A(val x: Int = when (baz()) { 1 -> bar(); else -> 0 })

fun box(): String {
    global = ""
    val a1 = A(10)
    assertEquals(10, a1.x)
    assertEquals("", global)

    val a2 = A()
    assertEquals(100, a2.x)
    assertEquals(":bar:", global)

    return "OK"
}