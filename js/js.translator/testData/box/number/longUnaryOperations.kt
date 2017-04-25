// EXPECTED_REACHABLE_NODES: 491
package foo

fun box(): String {

    var x: Long = 2L
    x++
    assertEquals(3L, x)
    ++x
    assertEquals(4L, x)

    var y = x++
    assertEquals(4L, y)
    assertEquals(5L, x)

    y = ++x
    assertEquals(6L, y)
    assertEquals(6L, x)

    x--
    assertEquals(5L, x)
    --x
    assertEquals(4L, x)

    y = +x
    assertEquals(4L, y)

    y = -x
    assertEquals(-4L, y)

    return "OK"
}
