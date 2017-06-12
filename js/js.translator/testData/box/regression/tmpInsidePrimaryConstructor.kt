// EXPECTED_REACHABLE_NODES: 542
package foo

val x: Int?
    get() = null

// Note: `x ?: 2` expression used to force to create tempary variable

class A {
    val a = x ?: 2
}

enum class E(val a: Int = 0) {
    X(),
    Y() {
        val y = x ?: 4

        override fun value() = y
    };

    val e = x ?: 3

    open fun value() = e
}

open class B(val b: Int)

class C : B(x ?: 6)


fun box(): String {
    assertEquals(2, A().a)
    assertEquals(3, E.X.e)
    assertEquals(4, E.Y.value())
    assertEquals(6, C().b)

    return "OK"
}
