package kotlin

external fun kotlinclib_intToByte(value: Int): Byte
external fun kotlinclib_intToChar(value: Int): Char
external fun kotlinclib_intToShort(value: Int): Short
external fun kotlinclib_intToLong(value: Int): Long
external fun kotlinclib_intToFloat(value: Int): Float
external fun kotlinclib_intToDouble(value: Int): Double

fun Int.toByte(): Byte {
    return kotlinclib_intToByte(this)
}

fun Int.toInt(): Int {
    return this
}

fun Int.toChar(): Char {
    return kotlinclib_intToChar(this)
}

fun Int.toShort(): Short {
    return kotlinclib_intToShort(this)
}

fun Int.toLong(): Long {
    return kotlinclib_intToLong(this)
}

fun Int.toFloat(): Float {
    return kotlinclib_intToFloat(this)
}

fun Int.toDouble(): Double {
    return kotlinclib_intToDouble(this)
}
