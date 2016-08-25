package kotlin

external fun kotlinclib_longToByte(value: Long): Byte
external fun kotlinclib_longToChar(value: Long): Char
external fun kotlinclib_longToShort(value: Long): Short
external fun kotlinclib_longToInt(value: Long): Int
external fun kotlinclib_longToFloat(value: Long): Float
external fun kotlinclib_longToDouble(value: Long): Double

fun Long.toByte(): Byte {
    return kotlinclib_longToByte(this)
}

fun Long.toLong(): Long {
    return this
}

fun Long.toChar(): Char {
    return kotlinclib_longToChar(this)
}

fun Long.toShort(): Short {
    return kotlinclib_longToShort(this)
}

fun Long.toInt(): Int {
    return kotlinclib_longToInt(this)
}

fun Long.toFloat(): Float {
    return kotlinclib_longToFloat(this)
}

fun Long.toDouble(): Double {
    return kotlinclib_longToDouble(this)
}
