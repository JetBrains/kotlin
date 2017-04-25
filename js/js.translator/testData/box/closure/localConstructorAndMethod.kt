// EXPECTED_REACHABLE_NODES: 501
package foo

interface B {
    fun result(): Int
}

class A(private val x: Int) {
    fun test() = object : B {
        val y = x + 1

        override fun result() = x * 10 + y
    }
}

fun box(): String {
    assertEquals(23, A(2).test().result())
    return "OK"
}