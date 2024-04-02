/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalStdlibApi::class)

package kotlin

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal expect fun secureRandomUUID(): UUID

private fun ByteArray.toLong(startIndex: Int): Long {
    return ((this[startIndex + 0].toLong() and 0xFF) shl 56) or
            ((this[startIndex + 1].toLong() and 0xFF) shl 48) or
            ((this[startIndex + 2].toLong() and 0xFF) shl 40) or
            ((this[startIndex + 3].toLong() and 0xFF) shl 32) or
            ((this[startIndex + 4].toLong() and 0xFF) shl 24) or
            ((this[startIndex + 5].toLong() and 0xFF) shl 16) or
            ((this[startIndex + 6].toLong() and 0xFF) shl 8) or
            (this[startIndex + 7].toLong() and 0xFF)
}

internal inline fun uuidFromBytes(bytes: ByteArray, block: (Long, Long) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    require(bytes.size == 16) { "Expected exactly 16 bytes" }
    block(bytes.toLong(startIndex = 0), bytes.toLong(startIndex = 8))
}

internal fun uuidToString(uuid: UUID, upperCase: Boolean): String {
    val format = if (upperCase) HexFormat.UpperCase else HexFormat.Default

    with(uuid) {
        val part1 = (msb shr 32).toInt().toHexString(format)
        val part2 = (msb shr 16).toShort().toHexString(format)
        val part3 = msb.toShort().toHexString(format)
        val part4 = (lsb shr 48).toShort().toHexString(format)
        val part5a = (lsb shr 32).toShort().toHexString(format)
        val part5b = lsb.toInt().toHexString(format)

        return "$part1-$part2-$part3-$part4-$part5a$part5b"
    }
}

internal inline fun uuidFromString(uuidString: String, block: (Long, Long) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val msb: Long
    val lsb: Long
    if (!uuidString.contains('-')) {
        msb = uuidString.hexToLong(startIndex = 0, endIndex = 16)
        lsb = uuidString.hexToLong(startIndex = 16, endIndex = 32)
    } else {
        val part1 = uuidString.hexToLong(startIndex = 0, endIndex = 8)
        uuidString.checkHyphenAt(8)
        val part2 = uuidString.hexToLong(startIndex = 9, endIndex = 13)
        uuidString.checkHyphenAt(13)
        val part3 = uuidString.hexToLong(startIndex = 14, endIndex = 18)
        uuidString.checkHyphenAt(18)
        val part4 = uuidString.hexToLong(startIndex = 19, endIndex = 23)
        uuidString.checkHyphenAt(23)
        val part5 = uuidString.hexToLong(startIndex = 24, endIndex = 36)

        msb = (part1 shl 32) or (part2 shl 16) or part3
        lsb = (part4 shl 48) or part5
    }
    return block(msb, lsb)
}

private fun String.checkHyphenAt(index: Int) {
    require(this[8] == '-') { "Expected '-' (hyphen) at index 8, but was ${this[index]}" }
}

internal fun uuidToHexString(uuid: UUID, upperCase: Boolean): String {
    val format = if (upperCase) HexFormat.UpperCase else HexFormat.Default
    with(uuid) { return msb.toHexString(format) + lsb.toHexString(format) }
}

internal fun uuidVersion(uuid: UUID): Int {
    if (!uuid.isIETFVariant) throw UnsupportedOperationException("Version is defined only for IETF variant")
    with(uuid) { return ((msb shr 12) and 0xF).toInt() }
}

private fun Long.toByteArray(bytes: ByteArray, startIndex: Int) {
    for (index in 0 until 8) {
        bytes[startIndex + index] = (this shr 8 * (7 - index)).toByte()
    }
}

internal fun uuidToByteArray(uuid: UUID): ByteArray {
    with(uuid) {
        val bytes = ByteArray(16)
        msb.toByteArray(bytes, 0)
        lsb.toByteArray(bytes, 8)
        return bytes
    }
}

internal val UUID_TIMESTAMP_ORDER = Comparator<UUID> { a, b ->
    require(a.version == b.version) { "Different UUID versions" }
    require(a.version != 1 && a.version != 6 && a.version != 7) { "Only UUID versions 1, 6 and 7 can be compared with their timestamp" }
    throw NotImplementedError()
}

internal val UUID_BITWISE_ORDER = Comparator<UUID> { a, b ->
    if (a.msb != b.msb)
        a.msb.toULong().compareTo(b.msb.toULong())
    else
        a.lsb.toULong().compareTo(b.lsb.toULong())
}

