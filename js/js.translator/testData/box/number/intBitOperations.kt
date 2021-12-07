// DONT_TARGET_EXACT_BACKEND: JS

fun testShl() {
    // wrapper prevents the constants folding
    infix fun Int.myShl(y: Int): Int = this shl y

    assertEquals(0 myShl 0, 0)
    assertEquals(0 myShl 1, 0)
    assertEquals(1 myShl 1, 2)
    assertEquals(3 myShl 1, 6)

    assertEquals(-1 myShl 0, -1)
    assertEquals(-1 myShl 1, -2)
    assertEquals(-1 myShl 2, -4)

    assertEquals(Int.MIN_VALUE myShl 0, Int.MIN_VALUE)
    assertEquals(Int.MIN_VALUE myShl 1, 0)

    assertEquals(Int.MAX_VALUE myShl 0, Int.MAX_VALUE)
    assertEquals(Int.MAX_VALUE myShl 1, -2)
}

fun testShr() {
    // wrapper prevents the constants folding
    infix fun Int.myShr(y: Int): Int = this shr y

    assertEquals(0 myShr 0, 0)
    assertEquals(0 myShr 1, 0)
    assertEquals(1 myShr 1, 0)
    assertEquals(3 myShr 1, 1)

    assertEquals(-1 myShr 0, -1)
    assertEquals(-1 myShr 1, -1)
    assertEquals(-1 myShr 2, -1)

    assertEquals(Int.MIN_VALUE myShr 0, Int.MIN_VALUE)
    assertEquals(Int.MIN_VALUE myShr 1, Int.MIN_VALUE / 2)

    assertEquals(Int.MAX_VALUE myShr 0, Int.MAX_VALUE)
    assertEquals(Int.MAX_VALUE myShr 1, 0x3FFFFFFF)
}

fun testUshr() {
    // wrapper prevents the constants folding
    infix fun Int.myUshr(y: Int): Int = this ushr y

    assertEquals(0 myUshr 0, 0)
    assertEquals(0 myUshr 1, 0)
    assertEquals(1 myUshr 1, 0)
    assertEquals(3 myUshr 1, 1)

    assertEquals(-1 myUshr 0, -1)
    assertEquals(-1 myUshr 1, 0x7FFFFFFF)
    assertEquals(-1 myUshr 2, 0x3FFFFFFF)

    assertEquals(Int.MIN_VALUE myUshr 0, Int.MIN_VALUE)
    assertEquals(Int.MIN_VALUE myUshr 1, 0x40000000)

    assertEquals(Int.MAX_VALUE myUshr 0, Int.MAX_VALUE)
    assertEquals(Int.MAX_VALUE myUshr 1, 0x3FFFFFFF)
}

fun box(): String {
    testShl()
    testShr()
    testUshr()

    return "OK"
}
