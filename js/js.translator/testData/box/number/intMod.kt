// DONT_TARGET_EXACT_BACKEND: JS

fun testModNegativeInt() {
    // wrapper prevents the constants folding
    infix fun Int.myModInt(y: Int): Any = this % y

    assertEquals(-1 myModInt -1, 0)
    assertEquals(-2 myModInt -1, 0)
    assertEquals(-2 myModInt -2, 0)
    assertEquals(-2 myModInt -3, -2)
    assertEquals(Int.MIN_VALUE myModInt -1, 0)
    assertEquals(Int.MIN_VALUE myModInt -2, 0)
    assertEquals(Int.MIN_VALUE myModInt -3, -2)
    assertEquals(Int.MIN_VALUE myModInt Int.MIN_VALUE, 0)
}

fun testModNegativeByte() {
    // wrapper prevents the constants folding
    infix fun Byte.myModByte(y: Byte): Any = this % y

    assertEquals((-1).toByte() myModByte (-1).toByte(), (0).toByte())
    assertEquals((-2).toByte() myModByte (-1).toByte(), (0).toByte())
    assertEquals((-2).toByte() myModByte (-2).toByte(), (0).toByte())
    assertEquals((-2).toByte() myModByte (-3).toByte(), (-2).toByte())
    assertEquals(Byte.MIN_VALUE myModByte (-1).toByte(), (0).toByte())
    assertEquals(Byte.MIN_VALUE myModByte (-2).toByte(), (0).toByte())
    assertEquals(Byte.MIN_VALUE myModByte (-3).toByte(), (-2).toByte())
    assertEquals(Byte.MIN_VALUE myModByte Byte.MIN_VALUE, (0).toByte())
}

fun testModNegativeShort() {
    // wrapper prevents the constants folding
    infix fun Short.myModShort(y: Short): Any = this % y

    assertEquals((-1).toShort() myModShort (-1).toShort(), (0).toShort())
    assertEquals((-2).toShort() myModShort (-1).toShort(), (0).toShort())
    assertEquals((-2).toShort() myModShort (-2).toShort(), (0).toShort())
    assertEquals((-2).toShort() myModShort (-3).toShort(), (-2).toShort())
    assertEquals(Short.MIN_VALUE myModShort (-1).toShort(), (0).toShort())
    assertEquals(Short.MIN_VALUE myModShort (-2).toShort(), (0).toShort())
    assertEquals(Short.MIN_VALUE myModShort (-3).toShort(), (-2).toShort())
    assertEquals(Short.MIN_VALUE myModShort Short.MIN_VALUE, (0).toShort())
}

fun testModNegativeLong() {
    // wrapper prevents the constants folding
    infix fun Long.myModLong(y: Long): Any = this % y

    assertEquals(-1L myModLong -1L, 0L)
    assertEquals(-2L myModLong -1L, 0L)
    assertEquals(-2L myModLong -2L, 0L)
    assertEquals(-2L myModLong -3L, -2L)
    assertEquals(Long.MIN_VALUE myModLong -1L, 0L)
    assertEquals(Long.MIN_VALUE myModLong -2L, 0L)
    assertEquals(Long.MIN_VALUE myModLong -3L, -2L)
    assertEquals(Long.MIN_VALUE myModLong Long.MIN_VALUE, 0L)
}

fun box(): String {
    testModNegativeInt()
    testModNegativeByte()
    testModNegativeShort()
    testModNegativeLong()

    return "OK"
}
