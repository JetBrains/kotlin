/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.wasm.internal

internal expect fun itoa32(inputValue: Int): String

internal expect fun utoa32(inputValue: UInt): String

internal expect fun itoa64(inputValue: Long): String

internal expect fun utoa64(inputValue: ULong): String

private const val ZeroCharCode = 0x30

internal fun digitToChar(input: Int): Char {
    assert(input in 0..9)
    return (ZeroCharCode + input).toChar()
}

internal fun decimalCount32(value: UInt): Int {
    if (value < 100000u) {
        if (value < 100u) {
            return 1 + (value >= 10u).toInt()
        } else {
            return 3 + (value >= 10000u).toInt() + (value >= 1000u).toInt()
        }
    } else {
        if (value < 10000000u) {
            return 6 + (value >= 1000000u).toInt()
        } else {
            return 8 + (value >= 1000000000u).toInt() + (value >= 100000000u).toInt()
        }
    }
}
