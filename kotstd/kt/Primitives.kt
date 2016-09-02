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
