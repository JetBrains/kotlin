import kotlin.test.*

fun box(): String {
    assertEquals(-2147483648, 1 shl -1)
    assertEquals(0, 1 shr -1)
    assertEquals(1, 1 shl 32)
    assertEquals(1073741823, -1 ushr 2)
    assertEquals(-1, -1 shr 2)

    return "OK"
}
