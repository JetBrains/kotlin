import kotlin.test.*

fun box(): String {
    assertEquals(97.0, 'a'.toDouble())
    assertEquals(-1, Char.MAX_VALUE.toShort())
    assertEquals(32768, Short.MIN_VALUE.toChar().toInt())
    assertEquals(-1, Char.MAX_VALUE.toByte())
    assertEquals(65408, Byte.MIN_VALUE.toChar().toInt())
    assertEquals(0, Float.MIN_VALUE.toChar().toInt())

    return "OK"
}
