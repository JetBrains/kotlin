// EXPECTED_REACHABLE_NODES: 504
package foo

open class Base {
    val c: Int

    constructor(a: Int, b: Int = 3) {
        c = a + b
    }
}

class Derived : Base {
    constructor(a: Int) : super(a)
}

fun box(): String {
    assertEquals(5, Derived(2).c)
    return "OK"
}