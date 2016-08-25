package kotlin

external fun kotlinclib_byteToChar(value: Byte): Char
external fun kotlinclib_byteToShort(value: Byte): Short
external fun kotlinclib_byteToInt(value: Byte): Int
external fun kotlinclib_byteToLong(value: Byte): Long
external fun kotlinclib_byteToFloat(value: Byte): Float
external fun kotlinclib_byteToDouble(value: Byte): Double


fun Byte.toByte(): Byte {
    return this
}

fun Byte.toInt(): Int {
    return kotlinclib_byteToInt(this)
}

fun Byte.toChar(): Char {
    return kotlinclib_byteToChar(this)
}

fun Byte.toShort(): Short {
    return kotlinclib_byteToShort(this)
}

fun Byte.toLong(): Long {
    return kotlinclib_byteToLong(this)
}

fun Byte.toFloat(): Float {
    return kotlinclib_byteToFloat(this)
}

fun Byte.toDouble(): Double {
    return kotlinclib_byteToDouble(this)
}