// KIND: STANDALONE
// MODULE: Smokes
// FILE: smoke0.kt
fun fooByte(): Byte {
    return -1
}

fun fooShort(): Short {
    return -1
}

fun fooInt(): Int {
    return -1
}

fun fooLong(): Long {
    return -1
}

fun fooUByte(): UByte {
    return 0u
}

fun fooUShort(): UShort {
    return 0u
}

fun fooUInt(): UInt {
    return 0u
}

fun fooULong(): ULong {
    return 0u
}

// FILE: smokes.kt
package org.kotlin

fun plus(a: Int, b: Int, c: Int) =
    a + b + c

fun logicalOr(a: Boolean, b: Boolean) = a || b

fun xor(a: Boolean, b: Boolean) = a xor b

fun plus(a: UByte, b: UByte) = a + b
fun minus(a: UByte, b: UByte) = a - b

fun plus(a: UShort, b: UShort) = a + b
fun minus(a: UShort, b: UShort) = a - b

fun plus(a: UInt, b: UInt) = a + b
fun minus(a: UInt, b: UInt) = a - b

fun plus(a: ULong, b: ULong) = a + b
fun minus(a: ULong, b: ULong) = a - b
