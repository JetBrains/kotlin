// http://youtrack.jetbrains.com/issue/KT-5345
// KT-5345 (Javascript) Type mismatch on Int / Float division
// If any of Number operands is floating-point, the result should be float too.

package foo

fun box(): String {
    assertEquals(0.5f, 1 / 2.0f, "Int / Float")
    assertEquals(0.5, 1 / 2.0, "Int / Double")

    assertEquals(0.5f, (1: Short) / 2.0f, "Short / Float")
    assertEquals(0.5, (1: Short) / 2.0, "Short / Double")

    assertEquals(0.5f, (1: Byte) / 2.0f, "Byte / Float")
    assertEquals(0.5, (1: Byte) / 2.0, "Byte / Double")

    return "OK"
}