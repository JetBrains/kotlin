// EXPECTED_REACHABLE_NODES: 492
package foo

fun box(): String {
    var a: Long = 23
    var b: Long = 200 + 30
    var c: Long = id((2000).plus(300))

    assertEquals(24L, a + 1)
    assertEquals(231L, b + 1)
    assertEquals(2301L, c + 1)

    return "OK"
}

fun id(x: Long) = x