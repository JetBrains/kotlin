package foo

fun testForNumber(numberX: Number) {
    assertEquals(true, 65.0 == numberX.toDouble())
    assertEquals(true, 65.0f == numberX.toFloat())
    assertEquals(true, 65 == numberX.toInt())
    assertEquals(true, (65: Short) == numberX.toShort())
    assertEquals(true, (65: Byte) == numberX.toByte())
}

fun box(): String {

    testForNumber(65.0)
    testForNumber(65.0f)
    testForNumber(65)
    testForNumber(65: Short)
    testForNumber(65: Byte)

    var doubleX: Double = 65.0
    assertEquals(true, 65.0 == doubleX.toDouble())
    assertEquals(true, 65.0f == doubleX.toFloat())
    assertEquals(true, 65 == doubleX.toInt())
    assertEquals(true, (65: Short) == doubleX.toShort())
    assertEquals(true, (65: Byte) == doubleX.toByte())

    var floatX: Float = 65.0f
    assertEquals(true, 65.0 == floatX.toDouble())
    assertEquals(true, 65.0f == floatX.toFloat())
    assertEquals(true, 65 == floatX.toInt())
    assertEquals(true, (65: Short) == floatX.toShort())
    assertEquals(true, (65: Byte) == floatX.toByte())

    val intX: Int = 65
    assertEquals(true, 65.0 == intX.toDouble())
    assertEquals(true, 65.0f == intX.toFloat())
    assertEquals(true, 65 == intX.toInt())
    assertEquals(true, (65: Short) == intX.toShort())
    assertEquals(true, (65: Byte) == intX.toByte())

    val shortX: Short = 65: Short
    assertEquals(true, 65.0 == shortX.toDouble())
    assertEquals(true, 65.0f == shortX.toFloat())
    assertEquals(true, 65 == shortX.toInt())
    assertEquals(true, (65: Short) == shortX.toShort())
    assertEquals(true, (65: Byte) == shortX.toByte())

    val byteX: Byte = 65: Byte
    assertEquals(true, 65.0 == byteX.toDouble())
    assertEquals(true, 65.0f == byteX.toFloat())
    assertEquals(true, 65 == byteX.toInt())
    assertEquals(true, (65: Short) == byteX.toShort())
    assertEquals(true, (65: Byte) == byteX.toByte())

    return "OK"
}