// EXPECTED_REACHABLE_NODES: 496
package foo

var counter = 0

class A {
    val x: Any?
        get() =
            when (counter++) {
                0 -> "0"
                1 -> null
                else -> counter
            }
}

fun box(): String {
    assertEquals(false, A().x is Int?, "(1)")
    assertEquals(true, A().x is Int?, "(2)")
    assertEquals(true, A().x is Int?, "(3)")
    assertEquals(3, counter)

    return "OK"
}