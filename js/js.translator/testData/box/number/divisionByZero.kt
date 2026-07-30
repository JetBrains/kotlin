fun intDivByZero(a: Int, b: Int): Int = a / b
fun intRemByZero(a: Int, b: Int): Int = a % b
fun byteDivByZero(a: Byte, b: Byte): Int = a / b
fun byteRemByZero(a: Byte, b: Byte): Int = a % b
fun shortDivByZero(a: Short, b: Short): Int = a / b
fun shortRemByZero(a: Short, b: Short): Int = a % b
fun longDivByZero(a: Long, b: Long): Long = a / b
fun longRemByZero(a: Long, b: Long): Long = a % b
fun floatDivByZero(a: Float, b: Float): Float = a / b
fun floatRemByZero(a: Float, b: Float): Float = a % b
fun doubleDivByZero(a: Double, b: Double): Double = a / b
fun doubleRemByZero(a: Double, b: Double): Double = a % b

fun assertThrowsArithmeticException(block: () -> Unit): Boolean {
    try {
        block()
        return false
    } catch (e: ArithmeticException) {
        return true
    }
}

fun box(): String {
    if (!assertThrowsArithmeticException { intDivByZero(1, 0) }) return "fail: Int div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { intRemByZero(1, 0) }) return "fail: Int rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { byteDivByZero(1, 0) }) return "fail: Byte div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { byteRemByZero(1, 0) }) return "fail: Byte rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { shortDivByZero(1, 0) }) return "fail: Short div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { shortRemByZero(1, 0) }) return "fail: Short rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { longDivByZero(1L, 0L) }) return "fail: Long div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { longRemByZero(1L, 0L) }) return "fail: Long rem by zero should throw ArithmeticException"


    if (floatDivByZero(1f, 0f) != Float.POSITIVE_INFINITY) return "fail: Float div by zero should equals to Float.POSITIVE_INFINITY"
    if (!floatRemByZero(1f, 0f).isNaN()) return "fail: Float div by zero should be Float.NaN"
    if (doubleDivByZero(1.0, 0.0) != Double.POSITIVE_INFINITY) return "fail: Double div by zero should equals to Double.POSITIVE_INFINITY"
    if (!doubleRemByZero(1.0, 0.0f).isNaN()) return "fail: Double div by zero should be Double.NaN"

    return "OK"
}
