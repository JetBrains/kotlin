/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.uuid

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.InlineOnly

/**
 * Represents a Universally Unique Identifier (UUID), also known as a Globally Unique Identifier (GUID).
 *
 * A UUID is a 128-bit value used to uniquely identify items across the globe. For a deeper understanding
 * of the bits layout and their meaning, refer to [RFC 9562](https://www.rfc-editor.org/rfc/rfc9562).
 *
 * The standard textual representation of a UUID is "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", where each 'x'
 * is a hexadecimal digit, e.g., "550e8400-e29b-41d4-a716-446655440000".
 *
 * This class provides utility functions for the creation and parsing of and operations on UUIDs.
 */
@SinceKotlin("2.0")
@ExperimentalStdlibApi
public class UUID internal constructor(
    @PublishedApi internal val mostSignificantBits: Long,
    @PublishedApi internal val leastSignificantBits: Long
) : Serializable {

    /**
     * Executes the specified block of code, providing access to the UUID's bits in the form of two [Long] values.
     *
     * This function is intended for use when one needs to perform bitwise operations with the UUID.
     *
     * The [action] will receive two [Long] arguments:
     *   - `mostSignificantBits`: The most significant 64 bits of this UUID presented in big-endian byte order.
     *   - `leastSignificantBits`: The least significant 64 bits of this UUID presented in big-endian byte order.
     * For example, for a UUID `550e8400-e29b-41d4-a716-446655440000`, the breakdown is the following:
     *   - `mostSignificantBits = 0x550e8400e29b41d4L`.
     *   - `leastSignificantBits = 0xa716446655440000uL.toLong()`.
     *
     * For example, to retrieve the [version number](https://www.rfc-editor.org/rfc/rfc9562.html#section-4.2) of this UUID:
     * ```kotlin
     * val version = uuid.toLongs { mostSignificantBits, _ ->
     *     ((mostSignificantBits shr 12) and 0xF).toInt()
     * }
     * ```
     *
     * @param action A function that takes two [Long] arguments (mostSignificantBits, leastSignificantBits).
     *   This function is guaranteed to be called exactly once.
     * @return The result of [action].
     *
     * @see UUID.fromLongs
     */
    @InlineOnly
    public inline fun <T> toLongs(action: (mostSignificantBits: Long, leastSignificantBits: Long) -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }
        return action(mostSignificantBits, leastSignificantBits)
    }

    /**
     * Executes a specified block of code, providing access to the UUID's bits in the form of two [ULong] values.
     *
     * This function is intended for use when one needs to perform bitwise operations with the UUID.
     *
     * The [action] will receive two [ULong] arguments:
     *   - `mostSignificantBits`: The most significant 64 bits of this UUID presented in big-endian byte order.
     *   - `leastSignificantBits`: The least significant 64 bits of this UUID presented in big-endian byte order.
     * For example, for a UUID `550e8400-e29b-41d4-a716-446655440000`, the breakdown is the following:
     *   - `mostSignificantBits = 0x550e8400e29b41d4uL`.
     *   - `leastSignificantBits = 0xa716446655440000uL`.
     *
     * For example, to identify whether a UUID is of the [IETF variant (variant 2)](https://www.rfc-editor.org/rfc/rfc9562.html#section-4.1):
     * ```kotlin
     * val isIETFVariant = uuid.toULongs { _, leastSignificantBits ->
     *     (leastSignificantBits shr 62) == 2uL
     * }
     * ```
     *
     * @param action A function that takes two [ULong] arguments (mostSignificantBits, leastSignificantBits).
     *   This function is guaranteed to be called exactly once.
     * @return The result of [action].
     *
     * @see UUID.fromULongs
     */
    @InlineOnly
    public inline fun <T> toULongs(action: (mostSignificantBits: ULong, leastSignificantBits: ULong) -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }
        return action(mostSignificantBits.toULong(), leastSignificantBits.toULong())
    }

    /**
     * Returns the string representation of this UUID.
     *
     * The resulting string is in the format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
     * where 'x' represents a hexadecimal digit. It is in lowercase and consists of 36 characters.
     *
     * @see UUID.parse
     * @sample samples.uuid.UUIDs.toStringSample
     */
    override fun toString(): String {
        val bytes = ByteArray(36)
        leastSignificantBits.formatBytesInto(bytes, 24, 6)
        bytes[23] = '-'.code.toByte()
        (leastSignificantBits ushr 48).formatBytesInto(bytes, 19, 2)
        bytes[18] = '-'.code.toByte()
        mostSignificantBits.formatBytesInto(bytes, 14, 2)
        bytes[13] = '-'.code.toByte()
        (mostSignificantBits ushr 16).formatBytesInto(bytes, 9, 2)
        bytes[8] = '-'.code.toByte()
        (mostSignificantBits ushr 32).formatBytesInto(bytes, 0, 4)
        return bytes.decodeToString()
    }

    /**
     * Returns the hexadecimal string representation of this UUID without hyphens.
     *
     * The resulting string is in lowercase and consists of 32 characters.
     *
     * @see UUID.parseHex
     * @sample samples.uuid.UUIDs.toHexString
     */
    public fun toHexString(): String {
        val bytes = ByteArray(32)
        leastSignificantBits.formatBytesInto(bytes, 16, 8)
        mostSignificantBits.formatBytesInto(bytes, 0, 8)
        return bytes.decodeToString()
    }

    /**
     * Returns a byte array representation of this UUID.
     *
     * The returned array contains 16 bytes. Each byte in the array sequentially represents
     * the next 8 bits of the UUID, starting from the most significant 8 bits
     * in the first byte to the least significant 8 bits in the last byte.
     *
     * @see UUID.fromByteArray
     * @sample samples.uuid.UUIDs.toByteArray
     */
    public fun toByteArray(): ByteArray {
        val bytes = ByteArray(SIZE_BYTES)
        mostSignificantBits.toByteArray(bytes, 0)
        leastSignificantBits.toByteArray(bytes, 8)
        return bytes
    }

    /**
     * Checks whether this UUID is equal to the specified [other] object.
     *
     * @param other The object to compare with this UUID.
     * @return `true` if [other] is an instance of UUID, and consists of the same sequence of bits as this UUID;
     *         `false` otherwise.
     *
     * @sample samples.uuid.UUIDs.uuidEquals
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UUID) return false
        return mostSignificantBits == other.mostSignificantBits &&
                leastSignificantBits == other.leastSignificantBits
    }

    override fun hashCode(): Int {
        val x = mostSignificantBits xor leastSignificantBits
        return (x shr 32).toInt() xor x.toInt()
    }

    public companion object {
        /**
         * The UUID with all bits set to zero.
         *
         * This UUID can be used as a special value, for instance, as a placeholder for a non-null but not yet initialized variable.
         */
        public val NIL: UUID = UUID(0, 0)

        /** The number of bytes used to represent an instance of UUID in a binary form. */
        public const val SIZE_BYTES: Int = 16

        /** The number of bits used to represent an instance of UUID in a binary form. */
        public const val SIZE_BITS: Int = 128

        /**
         * Creates a UUID from specified 128 bits split into two 64-bit Longs.
         *
         * This function interprets the provided `Long` values in big-endian byte order.
         *
         * @param mostSignificantBits The most significant 64 bits of the UUID.
         * @param leastSignificantBits The least significant 64 bits of the UUID.
         * @return A new UUID based on the specified bits.
         *
         * @see UUID.toLongs
         * @sample samples.uuid.UUIDs.fromLongs
         */
        public fun fromLongs(mostSignificantBits: Long, leastSignificantBits: Long): UUID =
            UUID(mostSignificantBits, leastSignificantBits)

        /**
         * Creates a UUID from specified 128 bits split into two 64-bit ULongs.
         *
         * This function interprets the provided `ULong` values in big-endian byte order.
         *
         * @param mostSignificantBits The most significant 64 bits of the UUID.
         * @param leastSignificantBits The least significant 64 bits of the UUID.
         * @return A new UUID based on the specified bits.
         *
         * @see UUID.toULongs
         * @sample samples.uuid.UUIDs.fromULongs
         */
        public fun fromULongs(mostSignificantBits: ULong, leastSignificantBits: ULong): UUID =
            UUID(mostSignificantBits.toLong(), leastSignificantBits.toLong())

        /**
         * Creates a UUID from a byte array containing 128 bits split into 16 bytes.
         *
         * Each byte in the [byteArray] sequentially represents
         * the next 8 bits of the UUID, starting from the most significant 8 bits
         * in the first byte to the least significant 8 bits in the last byte.
         *
         * @param byteArray A 16-byte array containing the UUID bits.
         * @throws IllegalArgumentException If the size of the [byteArray] is not exactly 16.
         * @return A new UUID based on the specified bits.
         *
         * @see UUID.toByteArray
         * @sample samples.uuid.UUIDs.fromByteArray
         */
        public fun fromByteArray(byteArray: ByteArray): UUID {
            require(byteArray.size == SIZE_BYTES) { "Expected exactly $SIZE_BYTES bytes" }

            return UUID(byteArray.toLong(startIndex = 0), byteArray.toLong(startIndex = 8))
        }

        /**
         * Parses a UUID from a standard UUID string format.
         *
         * For a valid [uuidString] the following property holds:
         * ```kotlin
         * val uuid = UUID.parse(uuidString)
         * assertEquals(uuid.toString(), uuidString.lowercase())
         * ```
         *
         * @param uuidString A string in the format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", where each 'x'
         *   is a hexadecimal digit, either lowercase or uppercase.
         * @throws IllegalArgumentException If the [uuidString] is not a 36-character string in the standard UUID format.
         * @return A UUID equivalent to the specified UUID string.
         *
         * @see UUID.toString
         * @sample samples.uuid.UUIDs.parse
         */
        public fun parse(uuidString: String): UUID {
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

        /**
         * Parses a UUID from a hexadecimal UUID string without hyphens.
         *
         * For a valid [hexString] the following property holds:
         * ```kotlin
         * val uuid = UUID.parseHex(hexString)
         * assertEquals(uuid.toHexString(), hexString.lowercase())
         * ```
         *
         * @param hexString A 32-character hexadecimal string representing the UUID.
         * @throws IllegalArgumentException If the [hexString] is not a 32-character hexadecimal string.
         * @return A UUID represented by the specified hexadecimal string.
         *
         * @see UUID.toHexString
         * @sample samples.uuid.UUIDs.parseHex
         */
        public fun parseHex(hexString: String): UUID {
            require(hexString.length == 32) { "Expected a 32-char hexadecimal string." }

            val msb = hexString.hexToLong(startIndex = 0, endIndex = 16)
            val lsb = hexString.hexToLong(startIndex = 16, endIndex = 32)
            return UUID(msb, lsb)
        }

        /**
         * Generates a new random UUID instance.
         *
         * The returned UUID is of [IETF variant (variant 2)](https://www.rfc-editor.org/rfc/rfc9562.html#section-4.1)
         * and [version 4](https://www.rfc-editor.org/rfc/rfc9562.html#section-4.2).
         * It is generated using a cryptographically secure pseudorandom number generator (CSPRNG) available in the platform.
         * Thus, if the underlying system has not collected enough entropy, this function may hang until the strong entropy is collected and the CSPRNG is initialized.
         *
         * Note that the returned UUID is not recommended for use for cryptographic purposes.
         * Because version 4 UUID has a partially predictable bit pattern, and utilizes at most 122 bits of entropy, regardless of platform.
         * Additionally, on platforms that do not provide a CSPRNG, the subsequent calls of this function could return guessable UUIDs.
         *
         * @return A randomly generated UUID.
         *
         * @sample samples.uuid.UUIDs.random
         */
        public fun random(): UUID =
            secureRandomUUID()

        /**
         * A Comparator that lexically orders UUIDs.
         *
         * This comparator compares the given two 128-bit UUIDs bit by bit sequentially,
         * starting from the most significant bit to the least significant.
         * UUID `a` is considered less than `b` if, at the first position where corresponding bits of the two UUIDs differ,
         * the bit in `a` is zero and the bit in `b` is one.
         * Conversely, `a` is considered greater than `b` if, at the first differing position,
         * the bit in `a` is one and the bit in `b` is zero.
         * If no differing bits are found, the two UUIDs are considered equal.
         *
         * The result of the comparison of UUIDs `a` and `b` by this comparator is equivalent to:
         * ```kotlin
         * a.toString().compareTo(b.toString())
         * ```
         *
         * @sample samples.uuid.UUIDs.lexicalOrder
         */
        public val LEXICAL_ORDER: Comparator<UUID>
            get() = UUID_LEXICAL_ORDER
    }
}

@ExperimentalStdlibApi
internal expect fun secureRandomUUID(): UUID

@ExperimentalStdlibApi
internal fun uuidFromRandomBytes(randomBytes: ByteArray): UUID {
    randomBytes[6] = (randomBytes[6].toInt() and 0x0f).toByte() /* clear version        */
    randomBytes[6] = (randomBytes[6].toInt() or 0x40).toByte()  /* set to version 4     */
    randomBytes[8] = (randomBytes[8].toInt() and 0x3f).toByte() /* clear variant        */
    randomBytes[8] = (randomBytes[8].toInt() or 0x80).toByte()  /* set to IETF variant  */
    return UUID.fromByteArray(randomBytes)
}

@ExperimentalStdlibApi
private val UUID_LEXICAL_ORDER = Comparator<UUID> { a, b ->
    if (a.mostSignificantBits != b.mostSignificantBits)
        a.mostSignificantBits.toULong().compareTo(b.mostSignificantBits.toULong())
    else
        a.leastSignificantBits.toULong().compareTo(b.leastSignificantBits.toULong())
}

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

@ExperimentalStdlibApi
private fun Long.formatBytesInto(dst: ByteArray, dstOffset: Int, count: Int) {
    var long = this
    var dstIndex = dstOffset + 2 * count
    repeat(count) {
        val byte = (long and 0xFF).toInt()
        val byteDigits = BYTE_TO_LOWER_CASE_HEX_DIGITS[byte]
        dst[--dstIndex] = byteDigits.toByte()
        dst[--dstIndex] = (byteDigits shr 8).toByte()
        long = long shr 8
    }
}

private fun String.checkHyphenAt(index: Int) {
    require(this[index] == '-') { "Expected '-' (hyphen) at index 8, but was ${this[index]}" }
}

private fun Long.toByteArray(dst: ByteArray, dstOffset: Int) {
    for (index in 0 until 8) {
        val shift = 8 * (7 - index)
        dst[dstOffset + index] = (this ushr shift).toByte()
    }
}
