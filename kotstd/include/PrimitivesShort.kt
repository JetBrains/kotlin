package kotlin

external fun kotlinclib_shortToByte(value: Short): Byte
external fun kotlinclib_shortToChar(value: Short): Char
external fun kotlinclib_shortToInt(value: Short): Int
external fun kotlinclib_shortToLong(value: Short): Long
external fun kotlinclib_shortToFloat(value: Short): Float
external fun kotlinclib_shortToDouble(value: Short): Double


fun Short.toByte(): Byte {
    return kotlinclib_shortToByte(this)
}

fun Short.toInt(): Int {
    return kotlinclib_shortToInt(this)
}

fun Short.toChar(): Char {
    return kotlinclib_shortToChar(this)
}

fun Short.toShort(): Short {
    return this
}

fun Short.toLong(): Long {
    return kotlinclib_shortToLong(this)
}

fun Short.toFloat(): Float {
    return kotlinclib_shortToFloat(this)
}

fun Short.toDouble(): Double {
    return kotlinclib_shortToDouble(this)
}
