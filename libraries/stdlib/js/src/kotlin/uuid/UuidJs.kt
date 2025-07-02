/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.uuid

import kotlin.js.internal.boxedLong.BoxedLongApi

@ExperimentalUuidApi
internal actual fun secureRandomUuid(): Uuid {
    val randomBytes = ByteArray(16)
    js("crypto").getRandomValues(randomBytes)
    return uuidFromRandomBytes(randomBytes)
}

@ExperimentalUuidApi
internal actual fun serializedUuid(uuid: Uuid): Any =
    throw UnsupportedOperationException("Serialization is supported only in Kotlin/JVM")

// Avoid bitwise operations with Longs in JS
@ExperimentalUuidApi
@OptIn(BoxedLongApi::class)
internal actual fun ByteArray.getLongAt(index: Int): Long {
    return Long(
        high = this.getIntAt(index),
        low = this.getIntAt(index + 4),
    )
}

private fun ByteArray.getIntAt(index: Int): Int {
    return ((this[index + 0].toInt() and 0xFF) shl 24) or
            ((this[index + 1].toInt() and 0xFF) shl 16) or
            ((this[index + 2].toInt() and 0xFF) shl 8) or
            (this[index + 3].toInt() and 0xFF)
}

// Avoid bitwise operations with Longs in JS
@ExperimentalUuidApi
@OptIn(BoxedLongApi::class)
internal actual fun Long.formatBytesInto(dst: ByteArray, dstOffset: Int, startIndex: Int, endIndex: Int) {
    var dstIndex = dstOffset
    if (startIndex < 4) {
        dstIndex = this.high.formatBytesInto(dst, dstIndex, startIndex, endIndex.coerceAtMost(4))
    }
    if (endIndex > 4) {
        this.low.formatBytesInto(dst, dstIndex, (startIndex - 4).coerceAtLeast(0), endIndex - 4)
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun Int.formatBytesInto(dst: ByteArray, dstOffset: Int, startIndex: Int, endIndex: Int): Int {
    var dstIndex = dstOffset
    for (reversedIndex in 3 - startIndex downTo 4 - endIndex) {
        val shift = reversedIndex shl 3
        val byte = ((this shr shift) and 0xFF)
        val byteDigits = BYTE_TO_LOWER_CASE_HEX_DIGITS[byte]
        dst[dstIndex++] = (byteDigits shr 8).toByte()
        dst[dstIndex++] = byteDigits.toByte()
    }
    return dstIndex
}

// Avoid bitwise operations with Longs in JS
@ExperimentalUuidApi
@OptIn(BoxedLongApi::class)
internal actual fun ByteArray.setLongAt(index: Int, value: Long) {
    setIntAt(index, value.high)
    setIntAt(index + 4, value.low)
}

private fun ByteArray.setIntAt(index: Int, value: Int) {
    var i = index
    for (reversedIndex in 3 downTo 0) {
        val shift = reversedIndex shl 3
        this[i++] = (value shr shift).toByte()
    }
}

// Avoid bitwise operations with Longs in JS
@OptIn(ExperimentalStdlibApi::class)
@ExperimentalUuidApi
internal actual fun uuidParseHexDash(hexDashString: String): Uuid {
    // xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
    // 8 hex digits fit into an Int
    val part1 = hexDashString.hexToInt(startIndex = 0, endIndex = 8)
    hexDashString.checkHyphenAt(8)
    val part2 = hexDashString.hexToInt(startIndex = 9, endIndex = 13)
    hexDashString.checkHyphenAt(13)
    val part3 = hexDashString.hexToInt(startIndex = 14, endIndex = 18)
    hexDashString.checkHyphenAt(18)
    val part4 = hexDashString.hexToInt(startIndex = 19, endIndex = 23)
    hexDashString.checkHyphenAt(23)
    val part5a = hexDashString.hexToInt(startIndex = 24, endIndex = 28)
    val part5b = hexDashString.hexToInt(startIndex = 28, endIndex = 36)

    @OptIn(BoxedLongApi::class)
    val msb = Long(
        high = part1,
        low = (part2 shl 16) or part3
    )

    @OptIn(BoxedLongApi::class)
    val lsb = Long(
        high = (part4 shl 16) or part5a,
        low = part5b
    )
    return Uuid.fromLongs(msb, lsb)
}

// Avoid bitwise operations with Longs in JS
@OptIn(ExperimentalStdlibApi::class)
@ExperimentalUuidApi
internal actual fun uuidParseHex(hexString: String): Uuid {
    // 8 hex digits fit into an Int
    @OptIn(BoxedLongApi::class)
    val msb = Long(
        high = hexString.hexToInt(startIndex = 0, endIndex = 8),
        low = hexString.hexToInt(startIndex = 8, endIndex = 16)
    )

    @OptIn(BoxedLongApi::class)
    val lsb = Long(
        high = hexString.hexToInt(startIndex = 16, endIndex = 24),
        low = hexString.hexToInt(startIndex = 24, endIndex = 32)
    )
    return Uuid.fromLongs(msb, lsb)
}
