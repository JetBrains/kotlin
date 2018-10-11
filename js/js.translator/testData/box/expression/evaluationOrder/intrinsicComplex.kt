// EXPECTED_REACHABLE_NODES: 1282
package foo

fun box(): String {
    var x = 0
    assertEquals(false, ++x > x)
    assertEquals(false, ++x > try {x} finally {})

    assertEquals(false, x++ > x)
    assertEquals(false, x++ > try {x} finally {})

    assertEquals(true, ++x == x)
    assertEquals(true, ++x == try {x} finally {})

    return "OK"
}