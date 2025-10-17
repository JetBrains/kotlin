/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.uuid

import kotlin.js.internal.boxedLong.BoxedLongApi

internal actual fun secureRandomBytes(destination: ByteArray): Unit {
    js("crypto").getRandomValues(destination)
}

@ExperimentalUuidApi
internal actual fun serializedUuid(uuid: Uuid): Any =
    throw UnsupportedOperationException("Serialization is supported only in Kotlin/JVM")

// Avoid bitwise operations with Longs in JS
@ExperimentalUuidApi
@OptIn(BoxedLongApi::class)  // Long constructor is intrinsified when BigInt-backed Longs are enabled.
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
@OptIn(BoxedLongApi::class) // Long's `high` and `low` properties are intrinsified when BigInt-backed Longs are enabled.
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
@IgnorableReturnValue
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
@OptIn(BoxedLongApi::class) // Long's `high` and `low` properties are intrinsified when BigInt-backed Longs are enabled.
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
@ExperimentalUuidApi
internal actual fun uuidParseHexDash(hexDashString: String): Uuid {
    return uuidParseHexDash(hexDashString) { inputString, errorDescription, errorIndex ->
        uuidThrowUnexpectedCharacterException(inputString, errorDescription, errorIndex)
    }
}

// Avoid bitwise operations with Longs in JS
@ExperimentalUuidApi
internal actual fun uuidParseHexDashOrNull(hexDashString: String): Uuid? {
    return uuidParseHexDash(hexDashString) { _, _, _ ->
        return null
    }
}

@ExperimentalUuidApi
internal inline fun uuidParseHexDash(
    hexDashString: String,
    onError: (inputString: String, errorDescription: String, errorIndex: Int) -> Nothing
): Uuid {
    val hexDigitExpectedMessage = "a hexadecimal digit"

    // xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
    // 8 hex digits fit into an Int
    val part1 = hexDashString.parseHexToInt(startIndex = 0, endIndex = 8) { onError(this, hexDigitExpectedMessage, it) }
    hexDashString.uuidCheckHyphenAt(8, onError)
    val part2 = hexDashString.parseHexToInt(startIndex = 9, endIndex = 13) { onError(this, hexDigitExpectedMessage, it) }
    hexDashString.uuidCheckHyphenAt(13, onError)
    val part3 = hexDashString.parseHexToInt(startIndex = 14, endIndex = 18) { onError(this, hexDigitExpectedMessage, it) }
    hexDashString.uuidCheckHyphenAt(18, onError)
    val part4 = hexDashString.parseHexToInt(startIndex = 19, endIndex = 23) { onError(this, hexDigitExpectedMessage, it) }
    hexDashString.uuidCheckHyphenAt(23, onError)
    val part5a = hexDashString.parseHexToInt(startIndex = 24, endIndex = 28) { onError(this, hexDigitExpectedMessage, it) }
    val part5b = hexDashString.parseHexToInt(startIndex = 28, endIndex = 36) { onError(this, hexDigitExpectedMessage, it) }

    @OptIn(BoxedLongApi::class) // Long constructor is intrinsified when BigInt-backed Longs are enabled.
    val msb = Long(
        high = part1,
        low = (part2 shl 16) or part3
    )

    @OptIn(BoxedLongApi::class) // Long constructor is intrinsified when BigInt-backed Longs are enabled.
    val lsb = Long(
        high = (part4 shl 16) or part5a,
        low = part5b
    )
    return Uuid.fromLongs(msb, lsb)
}

// Avoid bitwise operations with Longs in JS
@ExperimentalUuidApi
internal actual fun uuidParseHex(hexString: String): Uuid {
    return uuidParseHex(hexString) { inputString, errorDescription, errorIndex ->
        uuidThrowUnexpectedCharacterException(inputString, errorDescription, errorIndex)
    }
}

// Avoid bitwise operations with Longs in JS
@ExperimentalUuidApi
internal actual fun uuidParseHexOrNull(hexString: String): Uuid? {
    return uuidParseHex(hexString) { _, _, _ ->
        return null
    }
}

@ExperimentalUuidApi
private inline fun uuidParseHex(
    hexString: String,
    onError: (inputString: String, errorDescription: String, errorIndex: Int) -> Nothing
): Uuid {
    // 8 hex digits fit into an Int
    @OptIn(BoxedLongApi::class) // Long constructor is intrinsified when BigInt-backed Longs are enabled.
    val msb = Long(
        high = hexString.parseHexToInt(startIndex = 0, endIndex = 8) { onError(this, "a hexadecimal digit", it) },
        low = hexString.parseHexToInt(startIndex = 8, endIndex = 16) { onError(this, "a hexadecimal digit", it) }
    )

    @OptIn(BoxedLongApi::class) // Long constructor is intrinsified when BigInt-backed Longs are enabled.
    val lsb = Long(
        high = hexString.parseHexToInt(startIndex = 16, endIndex = 24) { onError(this, "a hexadecimal digit", it) },
        low = hexString.parseHexToInt(startIndex = 24, endIndex = 32) { onError(this, "a hexadecimal digit", it) }
    )
    return Uuid.fromLongs(msb, lsb)
}
