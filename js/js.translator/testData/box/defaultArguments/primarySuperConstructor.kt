// EXPECTED_REACHABLE_NODES: 1291
package foo

open class Base(a: Int, b: Int = 3) {
    val c: Int

    init {
        c = a + b
    }
}

class Derived(a: Int) : Base(a)

fun box(): String {
    assertEquals(5, Derived(2).c)
    return "OK"
}