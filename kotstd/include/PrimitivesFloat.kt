package kotlin

external fun kotlinclib_floatToByte(value: Float): Byte
external fun kotlinclib_floatToChar(value: Float): Char
external fun kotlinclib_floatToShort(value: Float): Short
external fun kotlinclib_floatToInt(value: Float): Int
external fun kotlinclib_floatToLong(value: Float): Long
external fun kotlinclib_floatToDouble(value: Float): Double

fun Float.toByte(): Byte {
    return kotlinclib_floatToByte(this)
}

fun Float.toChar(): Char {
    return kotlinclib_floatToChar(this)
}

fun Float.toShort(): Short {
    return kotlinclib_floatToShort(this)
}

fun Float.toInt(): Int {
    return kotlinclib_floatToInt(this)
}

fun Float.toLong(): Long {
    return kotlinclib_floatToLong(this)
}

fun Float.toFloat(): Float {
    return this
}

fun Float.toDouble(): Double {
    return kotlinclib_floatToDouble(this)
}
