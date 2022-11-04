/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.codec

// "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
private val base32EncodeMap = byteArrayOf(
    65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, /* 0 - 15 */
    81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 50, 51, 52, 53, 54, 55, /* 16 - 31 */
)

private val base32DecodeMap = byteArrayOf(
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 0 - 15 */
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 16 - 31 */
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 32 - 47 */
    -1, -1, 26, 27, 28, 29, 30, 31, -1, -1, -1, -1, -1, 32, -1, -1, /* 48 - 63 */
    -1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, /* 64 - 79 */
    15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, /* 80 - 95 */
)

// "0123456789ABCDEFGHIJKLMNOPQRSTUV"
private val base32HexEncodeMap = byteArrayOf(
    48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70, /* 0 - 15 */
    71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, /* 16 - 31 */
)

private val base32HexDecodeMap = byteArrayOf(
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 0 - 15 */
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 16 - 31 */
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 32 - 47 */
     0,  1,  2,  3,  4,  5,  6,  7,  8,  9, -1, -1, -1, 32, -1, -1, /* 48 - 63 */
    -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, /* 64 - 79 */
    25, 26, 27, 28, 29, 30, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 80 - 95 */
)

public object Base32 : BaseNCodec(base32EncodeMap, base32DecodeMap)

public fun Base32(extendedHex: Boolean): BaseNCodec = if (extendedHex)
    BaseNCodec(encodeMap = base32HexEncodeMap, base32HexDecodeMap)
else
    Base32
