/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.wasm.internal

private enum class CharCodes(val code: Int) {
//  PERCENT(0x25),
//  PLUS(0x2B),
  MINUS(0x2D),
//  DOT(0x2E),
  _0(0x30),
//  _1(0x31),
//  _2(0x32),
//  _3(0x33),
//  _4(0x34),
//  _5(0x35),
//  _6(0x36),
//  _7(0x37),
//  _8(0x38),
//  _9(0x39),
//  A(0x41),
//  B(0x42),
//  E(0x45),
//  I(0x49),
//  N(0x4E),
//  O(0x4F),
//  X(0x58),
//  Z(0x5A),
//  a(0x61),
//  b(0x62),
//  e(0x65),
//  n(0x6E),
//  o(0x6F),
//  u(0x75),
//  x(0x78),
//  z(0x7A)
}

private fun digitToChar(input: Int): Char {
    assert(input in 0..9)
    return (CharCodes._0.code + input).toChar()
}

// Inspired by the AssemblyScript implementation
internal fun itoa32(inputValue: Int, radix: Int): String {
    if (radix < 2 || radix > 36)
        throw IllegalArgumentException("Radix argument is unreasonable")

    if (radix != 10)
        TODO("When we need it")

    if (inputValue == 0) return "0"
    // We can't represent abs(Int.MIN_VALUE), so just hardcode it here
    if (inputValue == Int.MIN_VALUE) return "-2147483648"

    val sign = inputValue ushr 31
    assert(sign == 1 || sign == 0)
    val absValue = if (sign == 1) -inputValue else inputValue

    val decimals = decimalCount32(absValue) + sign
    val buf = CharArray(decimals)
    utoaDecSimple(buf, absValue, decimals)
    if (sign == 1)
        buf[0] = CharCodes.MINUS.code.toChar()

    return kotlin.String(buf)
}

private fun utoaDecSimple(buffer: CharArray, numInput: Int, offsetInput: Int) {
    assert(numInput != 0)
    assert(buffer.isNotEmpty())
    assert(offsetInput > 0 && offsetInput <= buffer.size)

    var num = numInput
    var offset = offsetInput
    do {
        val t = num / 10
        val r = num % 10
        num = t
        offset--
        buffer[offset] = digitToChar(r)
    } while (num > 0)
}

private fun utoaDecSimple64(buffer: CharArray, numInput: Long, offsetInput: Int) {
    assert(numInput != 0L)
    assert(buffer.isNotEmpty())
    assert(offsetInput > 0 && offsetInput <= buffer.size)

    var num = numInput
    var offset = offsetInput
    do {
        val t = num / 10
        val r = (num % 10).toInt()
        num = t
        offset--
        buffer[offset] = digitToChar(r)
    } while (num > 0)
}


private fun Boolean.toInt() = if (this) 1 else 0

private fun decimalCount32(value: Int): Int {
    if (value < 100000) {
        if (value < 100) {
            return 1 + (value >= 10).toInt()
        } else {
            return 3 + (value >= 10000).toInt() + (value >= 1000).toInt()
        }
    } else {
        if (value < 10000000) {
            return 6 + (value >= 1000000).toInt()
        } else {
            return 8 + (value >= 1000000000).toInt() + (value >= 100000000).toInt()
        }
    }
}

internal fun itoa64(inputValue: Long, radix: Int): String {
    if (inputValue in Int.MIN_VALUE..Int.MAX_VALUE)
        return itoa32(inputValue.toInt(), radix)

    if (radix < 2 || radix > 36)
        throw IllegalArgumentException("Radix argument is unreasonable")

    if (inputValue == 0L) return "0"
    // We can't represent abs(Long.MIN_VALUE), so just hardcode it here
    if (inputValue == Long.MIN_VALUE) return "-9223372036854775808"

    if (radix != 10) {
        TODO("When we need it")
    }

    val sign = (inputValue ushr 63).toInt()
    assert(sign == 1 || sign == 0)
    val absValue = if (sign == 1) -inputValue else inputValue

    val decimals = decimalCount64High(absValue) + sign
    val buf = CharArray(decimals)
    utoaDecSimple64(buf, absValue, decimals)
    if (sign == 1)
        buf[0] = CharCodes.MINUS.code.toChar()

    return kotlin.String(buf)
}

// Count number of decimals for u64 values
// In our case input value always greater than 2^32-1 so we can skip some parts
private fun decimalCount64High(value: Long): Int {
    if (value < 1000000000000000) {
        if (value < 1000000000000) {
            return 10 + (value >= 100000000000).toInt() + (value >= 10000000000).toInt()
        } else {
            return 13 + (value >= 100000000000000).toInt() + (value >= 10000000000000).toInt()
        }
    } else {
        if (value < 100000000000000000) {
            return 16 + (value >= 10000000000000000).toInt()
        } else {
            return 18 + (value >= 1000000000000000000).toInt()
        }
    }
}
