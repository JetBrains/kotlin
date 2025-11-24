/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

// Based on the AssemblyScript implementation [https://github.com/AssemblyScript/assemblyscript/blob/1e0466ef94fa5cacd0984e4f31a0087de51538a8/std/assembly/util/number.ts]

internal actual fun itoa32(inputValue: Int): String {
    if (inputValue == 0) return "0"

    val isNegative = inputValue < 0
    val absValue = if (isNegative) -inputValue else inputValue
    val absValueString = utoa32(absValue.toUInt())

    return if (isNegative) "-$absValueString" else absValueString
}

internal actual fun utoa32(inputValue: UInt): String {
    if (inputValue == 0U) return "0"

    val decimals = decimalCount32(inputValue)
    val buf = WasmCharArray(decimals)

    utoaDecSimple(buf, inputValue, decimals)

    return buf.createString()
}

private fun utoaDecSimple(buffer: WasmCharArray, numInput: UInt, offsetInput: Int) {
    assert(numInput != 0U)
    assert(buffer.len() > 0)
    assert(offsetInput > 0 && offsetInput <= buffer.len())

    var num = numInput
    var offset = offsetInput
    do {
        val t = num / 10U
        val r = num % 10U
        num = t
        offset--
        buffer.set(offset, digitToChar(r.toInt()))
    } while (num > 0U)
}

private fun utoaDecSimple64(buffer: WasmCharArray, numInput: ULong, offsetInput: Int) {
    assert(numInput != 0UL)
    assert(buffer.len() > 0)
    assert(offsetInput > 0 && offsetInput <= buffer.len())

    var num = numInput
    var offset = offsetInput
    do {
        val t = num / 10U
        val r = num % 10U
        num = t
        offset--
        buffer.set(offset, digitToChar(r.toInt()))
    } while (num > 0U)
}

internal actual fun itoa64(inputValue: Long): String {
    if (inputValue in Int.MIN_VALUE..Int.MAX_VALUE)
        return itoa32(inputValue.toInt())

    val isNegative = inputValue < 0
    val absValue = if (isNegative) -inputValue else inputValue
    val absValueString = utoa64(absValue.toULong())

    return if (isNegative) "-$absValueString" else absValueString
}

internal actual fun utoa64(inputValue: ULong): String {
    if (inputValue <= UInt.MAX_VALUE) return utoa32(inputValue.toUInt())
    val decimals = decimalCount64High(inputValue)
    val buf = WasmCharArray(decimals)

    utoaDecSimple64(buf, inputValue, decimals)

    return buf.createString()
}

// Count number of decimals for u64 values
// In our case input value always greater than 2^32-1 so we can skip some parts
private fun decimalCount64High(value: ULong): Int {
    if (value < 1000000000000000UL) {
        if (value < 1000000000000UL) {
            return 10 + (value >= 100000000000UL).toInt() + (value >= 10000000000UL).toInt()
        } else {
            return 13 + (value >= 100000000000000UL).toInt() + (value >= 10000000000000UL).toInt()
        }
    } else {
        if (value < 100000000000000000UL) {
            return 16 + (value >= 10000000000000000UL).toInt()
        } else {
            return 18 + (value >= 10000000000000000000UL).toInt() + (value >= 1000000000000000000UL).toInt()
        }
    }
}
