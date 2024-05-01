/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalStdlibApi::class)

package kotlin.uuid

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

internal fun uuidFromBytes(bytes: ByteArray): UUID {
    require(bytes.size == UUID.SIZE_BYTES) { "Expected exactly ${UUID.SIZE_BYTES} bytes" }
    return UUID(bytes.toLong(startIndex = 0), bytes.toLong(startIndex = 8))
}

internal fun uuidFromRandomBytes(randomBytes: ByteArray): UUID {
    randomBytes[6] = (randomBytes[6].toInt() and 0x0f).toByte() /* clear version        */
    randomBytes[6] = (randomBytes[6].toInt() or 0x40).toByte()  /* set to version 4     */
    randomBytes[8] = (randomBytes[8].toInt() and 0x3f).toByte() /* clear variant        */
    randomBytes[8] = (randomBytes[8].toInt() or 0x80).toByte()  /* set to IETF variant  */
    return UUID.fromByteArray(randomBytes)
}

internal fun uuidToString(uuid: UUID): String = with(uuid) {
    val part1 = (mostSignificantBits shr 32).toInt().toHexString()
    val part2 = (mostSignificantBits shr 16).toShort().toHexString()
    val part3 = mostSignificantBits.toShort().toHexString()
    val part4 = (leastSignificantBits shr 48).toShort().toHexString()
    val part5a = (leastSignificantBits shr 32).toShort().toHexString()
    val part5b = leastSignificantBits.toInt().toHexString()

    "$part1-$part2-$part3-$part4-$part5a$part5b"
}

internal fun uuidFromString(uuidString: String): UUID {
    require(uuidString.length == 36) { "Expected a 36-char string in the standard UUID format." }

    val part1 = uuidString.hexToLong(startIndex = 0, endIndex = 8)
    uuidString.checkHyphenAt(8)
    val part2 = uuidString.hexToLong(startIndex = 9, endIndex = 13)
    uuidString.checkHyphenAt(13)
    val part3 = uuidString.hexToLong(startIndex = 14, endIndex = 18)
    uuidString.checkHyphenAt(18)
    val part4 = uuidString.hexToLong(startIndex = 19, endIndex = 23)
    uuidString.checkHyphenAt(23)
    val part5 = uuidString.hexToLong(startIndex = 24, endIndex = 36)

    val msb = (part1 shl 32) or (part2 shl 16) or part3
    val lsb = (part4 shl 48) or part5
    return UUID(msb, lsb)
}

private fun String.checkHyphenAt(index: Int) {
    require(this[8] == '-') { "Expected '-' (hyphen) at index 8, but was ${this[index]}" }
}

internal fun uuidToHexString(uuid: UUID): String = with(uuid) {
    mostSignificantBits.toHexString() + leastSignificantBits.toHexString()
}

internal fun uuidFromHexString(hexString: String): UUID {
    require(hexString.length == 32) { "Expected a 32-char hexadecimal string." }
    val msb = hexString.hexToLong(startIndex = 0, endIndex = 16)
    val lsb = hexString.hexToLong(startIndex = 16, endIndex = 32)
    return UUID(msb, lsb)
}

private fun Long.toByteArray(bytes: ByteArray, startIndex: Int) {
    for (index in 0 until 8) {
        bytes[startIndex + index] = (this shr 8 * (7 - index)).toByte()
    }
}

internal fun uuidToByteArray(uuid: UUID): ByteArray {
    with(uuid) {
        val bytes = ByteArray(UUID.SIZE_BYTES)
        mostSignificantBits.toByteArray(bytes, 0)
        leastSignificantBits.toByteArray(bytes, 8)
        return bytes
    }
}

internal val UUID_LEXICAL_ORDER = Comparator<UUID> { a, b ->
    if (a.mostSignificantBits != b.mostSignificantBits)
        a.mostSignificantBits.toULong().compareTo(b.mostSignificantBits.toULong())
    else
        a.leastSignificantBits.toULong().compareTo(b.leastSignificantBits.toULong())
}
