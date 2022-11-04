/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.codec

// "0123456789ABCDEF"
private val base16EncodeMap = byteArrayOf(
    48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70, /* 0 - 15 */
)

private val base16DecodeMap = byteArrayOf(
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 0 - 15 */
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 16 - 31 */
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 32 - 47 */
     0,  1,  2,  3,  4,  5,  6,  7,  8,  9, -1, -1, -1, 16, -1, -1, /* 48 - 63 */
    -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 64 - 79 */
)

// "0123456789abcdef"
private val base16LowerEncodeMap = byteArrayOf(
    48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 97, 98, 99, 100, 101, 102, /* 0 - 15 */
)

private val base16LowerDecodeMap = byteArrayOf(
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 0 - 15 */
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 16 - 31 */
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 32 - 47 */
     0,  1,  2,  3,  4,  5,  6,  7,  8,  9, -1, -1, -1, 16, -1, -1, /* 48 - 63 */
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 64 - 79 */
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 80 - 95 */
    -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 96 - 111 */
)

public object Base16 : BaseNCodec(base16EncodeMap, base16DecodeMap)

public fun Base16(lowercase: Boolean): BaseNCodec = if (lowercase)
    BaseNCodec(encodeMap = base16LowerEncodeMap, base16LowerDecodeMap)
else
    Base16