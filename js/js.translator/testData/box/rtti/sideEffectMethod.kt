// EXPECTED_REACHABLE_NODES: 496
package foo

var counter = 0

class A {
    fun getCounter(): Any? =
        when (counter++) {
            0 -> "0"
            1 -> null
            else -> counter
        }
}

fun box(): String {
    assertEquals(false, A().getCounter() is Int?, "(1)")
    assertEquals(true, A().getCounter() is Int?, "(2)")
    assertEquals(true, A().getCounter() is Int?, "(3)")
    assertEquals(3, counter)

    return "OK"
}