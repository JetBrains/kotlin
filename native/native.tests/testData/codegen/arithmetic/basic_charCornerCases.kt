import kotlin.test.*

private fun charCornersMinus(): Int {
    val a: Char = 0xFFFF.toChar()
    val b: Char = 0.toChar()
    return a - b
}

private fun charCornersComparison(): Boolean {
    val a = 0xFFFF.toChar()
    val b = 0.toChar()
    return a < b
}

fun box(): String {
    assertEquals(65535, charCornersMinus())
    assertFalse(charCornersComparison())

    return "OK"
}
