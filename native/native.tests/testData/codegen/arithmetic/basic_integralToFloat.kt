import kotlin.test.*

fun box(): String {
    assertEquals(9.223372E18f, Long.MAX_VALUE.toFloat())
    assertEquals(-9.223372E18f, Long.MIN_VALUE.toFloat())

    assertEquals(-2.147483648E9, Int.MIN_VALUE.toDouble())
    assertEquals(2.147483647E9, Int.MAX_VALUE.toDouble())

    assertEquals(2147483647, Double.MAX_VALUE.toInt())
    assertEquals(0, Float.MIN_VALUE.toLong())

    assertEquals(9223372036854775807, Float.MAX_VALUE.toLong())
    assertEquals(0, Double.MIN_VALUE.toInt())

    return "OK"
}
