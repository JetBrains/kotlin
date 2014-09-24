package foo

fun box(): String {

    assertEquals(65, 321.0.toByte())
    assertEquals(-56, 200.0.toByte())

    assertEquals(65, 321.0f.toByte())
    assertEquals(-56, 200.0f.toByte())

    assertEquals(65, 321.toByte())
    assertEquals(-56, 200.toByte())

    assertEquals(65, (321: Short).toByte())
    assertEquals(-56, (200: Short).toByte())

    assertEquals(-1, 65535.0.toShort())
    assertEquals(-1, 65535.0f.toShort())
    assertEquals(-1, 65535.toShort())

    assertEquals(65535, 65535.2.toInt())
    assertEquals(23, 23.6f.toInt())
    assertEquals(-12, -12.4.toShort())
    assertEquals(-12, -12.4.toByte())

    return "OK"
}
