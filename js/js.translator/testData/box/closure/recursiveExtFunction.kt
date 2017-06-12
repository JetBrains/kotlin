// EXPECTED_REACHABLE_NODES: 496
package foo

fun Int.foo(a: Int): Int {
    if (a == 0) {
        return this.foo(4) + foo(7) + this
    }

    return a
}

fun box(): String {
    assertEquals(34, 23.foo(0))

    fun Int.bar(a: Int): Int {
        if (a == 0) {
            return this.bar(12) + bar(4) + this
        }

        return a
    }
    assertEquals(19, 3.bar(0))

    fun f() = 11
    val v = 3
    fun Int.baz(a: Int): Int {
        if (a == 0) {
            return this.baz(v) + baz(f()) + this
        }

        return a
    }
    assertEquals(21, 7.baz(0))

    return "OK"
}
