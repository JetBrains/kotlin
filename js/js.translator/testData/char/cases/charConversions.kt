package foo

fun box(): String {

    assertEquals('A', 'A'.toChar(), "toChar")
    assertEquals(65, 'A'.toInt(), "toInt")
    assertEquals(65: Short, 'A'.toShort(), "toShort")
    assertEquals(65: Byte, 'A'.toByte(), "toByte")
    assertEquals(65.0, 'A'.toDouble(), "toDouble")
    assertEquals(65.0f, 'A'.toFloat(), "toFloat")
    assertEquals(65L, 'A'.toLong(), "toLong")

    return "OK"
}