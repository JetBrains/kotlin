// EXPECTED_REACHABLE_NODES: 491
package foo

fun box(): String {
    var a: dynamic = 12
    var b: dynamic = 33.4
    var c: dynamic = "text"
    val d: dynamic = true

    assertEquals(11, --a)
    assertEquals(11, a)
    assertEquals(11, a--)
    assertEquals(10, a)
    assertEquals(10, a++)
    assertEquals(11, a)
    assertEquals(12, ++a)
    assertEquals(12, a)

    assertEquals(32.4, --b)
    assertEquals(32.4, b)
    assertEquals(32.4, b--)
    assertEquals(31.4, b)
    assertEquals(31.4, b++)
    assertEquals(32.4, b)
    assertEquals(33.4, ++b)
    assertEquals(33.4, b)

    return "OK"
}
