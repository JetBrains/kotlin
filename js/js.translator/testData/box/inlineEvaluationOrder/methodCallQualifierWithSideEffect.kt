// EXPECTED_REACHABLE_NODES: 499
package foo

// Test for KT-7502

class A(val value: Int) {
    fun plus(num: Int): Int = this.value + num
}

fun box(): String {
    assertEquals(15, A(fizz(5)).plus(buzz(10)))
    assertEquals("fizz(5);buzz(10);", pullLog())

    return "OK"
}