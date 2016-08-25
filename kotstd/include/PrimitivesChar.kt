package kotlin

external fun kotlinclib_charToByte(value: Char): Byte
external fun kotlinclib_charToShort(value: Char): Short
external fun kotlinclib_charToInt(value: Char): Int
external fun kotlinclib_charToLong(value: Char): Long
external fun kotlinclib_charToFloat(value: Char): Float
external fun kotlinclib_charToDouble(value: Char): Double


fun Char.toByte(): Byte {
    return kotlinclib_charToByte(this)
}

fun Char.toInt(): Int {
    return kotlinclib_charToInt(this)
}

fun Char.toChar(): Char {
    return this
}

fun Char.toShort(): Short {
    return kotlinclib_charToShort(this)
}

fun Char.toLong(): Long {
    return kotlinclib_charToLong(this)
}

fun Char.toFloat(): Float {
    return kotlinclib_charToFloat(this)
}

fun Char.toDouble(): Double {
    return kotlinclib_charToDouble(this)
}
