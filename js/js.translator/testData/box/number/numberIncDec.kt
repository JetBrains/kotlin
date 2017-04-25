// EXPECTED_REACHABLE_NODES: 491
package foo

fun box(): String {

    assertEquals(100.inc(), 101)
    assertEquals(100.dec(), 99)

    var x = 100
    assertEquals(x.inc(), 101)
    assertEquals(x, 100)

    assertEquals(x.dec(), 99)
    assertEquals(x, 100)

    return "OK"
}