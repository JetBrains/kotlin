package kotlin

external fun kotlinclib_doubleToByte(value: Double): Byte
external fun kotlinclib_doubleToChar(value: Double): Char
external fun kotlinclib_doubleToShort(value: Double): Short
external fun kotlinclib_doubleToInt(value: Double): Int
external fun kotlinclib_doubleToLong(value: Double): Long
external fun kotlinclib_doubleToFloat(value: Double): Float

fun Double.toByte(): Byte {
    return kotlinclib_doubleToByte(this)
}

fun Double.toChar(): Char {
    return kotlinclib_doubleToChar(this)
}

fun Double.toShort(): Short {
    return kotlinclib_doubleToShort(this)
}

fun Double.toInt(): Int {
  return kotlinclib_doubleToInt(this)
}

fun Double.toLong(): Long {
    return kotlinclib_doubleToLong(this)
}

fun Double.toFloat(): Float {
    return kotlinclib_doubleToFloat(this)
}

fun Double.toDouble(): Double {
  return this
}
