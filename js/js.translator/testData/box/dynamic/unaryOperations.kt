// EXPECTED_REACHABLE_NODES: 505
package foo

fun box(): String {
    val a: dynamic = 12
    var b: dynamic = 33.4
    var c: dynamic = "text"
    val d: dynamic = true

    assertEquals(-12, -a)
    assertEquals(33.4, +b)
    testTrue { d }
    testFalse { !d }
    testTrue { !!d }
    testFalse { !a }
    testTrue { !!a }
    testFalse { !b }
    testTrue { !!b }

    return "OK"
}
