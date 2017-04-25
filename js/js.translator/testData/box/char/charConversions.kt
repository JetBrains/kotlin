// EXPECTED_REACHABLE_NODES: 493
package foo

fun box(): String {

    assertEquals('A', 'A'.toChar(), "toChar")
    assertEquals(65, 'A'.toInt(), "toInt")
    assertEquals(65.toShort(), 'A'.toShort(), "toShort")
    assertEquals(65.toByte(), 'A'.toByte(), "toByte")
    assertEquals(65.0, 'A'.toDouble(), "toDouble")
    assertEquals(65.0f, 'A'.toFloat(), "toFloat")
    assertEquals(65L, 'A'.toLong(), "toLong")

    return "OK"
}