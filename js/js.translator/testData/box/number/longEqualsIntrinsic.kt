// EXPECTED_REACHABLE_NODES: 491
package foo

fun box(): String {
    assertEquals(true, 10L == 10L, "Long == Long")
    assertEquals(true, 10L != 11L, "Long != Long")

    val x1: Long? = 10L
    val x2: Long? = 10L
    assertEquals(false, x1 == null, "Long? == null")
    assertEquals(false, null == x1, "null == Long?")
    assertEquals(true, x1 == 10L, "Long? == Long")
    assertEquals(true, 10L == x1, "Long == Long?")
    assertEquals(true, x1 == x2, "Long? == Long?")

    val x3: Long? = null
    val x4: Long? = null
    assertEquals(true, x3 == null, "Long?(null) == null")
    assertEquals(true, null == x3, "null == Long?(null)")
    assertEquals(false, x3 == 10L, "Long?(null) == Long")
    assertEquals(false, 10L == x3, "Long == Long?(null)")
    assertEquals(false, x3 == x1, "Long?(null) == Long?")
    assertEquals(true, x3 == x4, "Long?(null) == Long?(null)")

    val number1: Number = 10L
    val number2: Number = 10L
    assertEquals(true, number1 == number2, "Number == Number")
    assertEquals(true, number1 == 10L, "Number == Long")
    assertEquals(true, number1 != 11L, "Number != Long")
    assertEquals(true, 10L == number1, "Long == Number")
    assertEquals(true, 11L != number1, "Long != Number")

    val y1: Any = 10L
    var y2: Any = 10
    var y3: Any? = null
    assertEquals(true, y1 == 10L, "Any == Long")
    assertEquals(true, 10L == y1, "Long == Any 1")
    assertEquals(false, 10L == y2, "Long == Any 2")
    assertEquals(false, 10L == y3, "Long == Any?(null)")
    assertEquals(true, x3 == y3, "Long?(null) == Any?(null)")

    return "OK"
}