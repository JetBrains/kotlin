object PrimitiveTypes {
    const val booleanFalse: Boolean = false
    const val booleanTrue: Boolean = true

    const val int0: Int = 0
    const val intMinus1000: Int = -1000
    const val intMinValue: Int = Int.MIN_VALUE
    const val intMaxValue: Int = Int.MAX_VALUE
    const val intHex: Int = 0xffffffff.toInt()

    const val byte0: Byte = 0.toByte()
    const val byte50: Byte = 50.toByte()

    const val short5: Short = 5.toShort()

    const val charC: Char = 'C'
    const val char0: Char = 0.toChar()
    const val char10: Char = 10.toChar()
    const val char13: Char = 13.toChar()

    const val long0: Long = 0L
    const val longMaxValue: Long = Long.MAX_VALUE
    const val longMinValue: Long = Long.MIN_VALUE
    const val longHex: Long = 0xffffffff

    const val float54 = 5.4f
    val floatMaxValue = Float.MAX_VALUE
    val floatNan = Float.NaN
    val floatPositiveInfinity = Float.POSITIVE_INFINITY
    val floatNegativeInfinity = Float.NEGATIVE_INFINITY

    const val double54 = 5.4
    val doubleMaxValue = Double.MAX_VALUE
    val doubleNan = Double.NaN
    val doublePositiveInfinity = Double.POSITIVE_INFINITY
    val doubleNegativeInfinity = Double.NEGATIVE_INFINITY

    const val stringHelloWorld: String = "Hello, world!"
    const val stringQuotes: String = "quotes \" ''quotes"
    const val stringRN: String = "\r\n"
}
