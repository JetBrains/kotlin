/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.wasm.internal

internal expect fun itoa32(inputValue: Int): String

internal expect fun utoa32(inputValue: UInt): String

internal expect fun itoa64(inputValue: Long): String

internal expect fun utoa64(inputValue: ULong): String

private enum class CharCodes(val code: Int) {
//  PERCENT(0x25),
    PLUS(0x2B),
    MINUS(0x2D),
    DOT(0x2E),
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
    e(0x65),
//  n(0x6E),
//  o(0x6F),
//  u(0x75),
//  x(0x78),
//  z(0x7A)
}

internal fun digitToChar(input: Int): Char {
    assert(input in 0..9)
    return (CharCodes._0.code + input).toChar()
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
