// EXPECTED_REACHABLE_NODES: 497
package foo

class A(val f: (B.() -> Int)?)

class B(val x: Int)

fun test(g: (B.() -> Int)?): Int? {
    val a = A(g)
    val b = B(2)
    return a.f?.invoke(b)
}

fun box(): String {
    assertEquals(5, test { x + 3 })
    return "OK"
}