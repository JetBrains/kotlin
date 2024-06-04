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
 * A UUID is a 128-bit value used to uniquely identify items universally. They are
 * particularly useful in environments lacking central registration authority or coordination
 * mechanism for generating identifiers, making UUIDs highly suitable for distributed systems.
 *
 * The standard textual representation of a UUID, also known as the "hex-and-dash" format, is:
 * "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", where 'x' represents a hexadecimal digit,
 * e.g., "550e8400-e29b-41d4-a716-446655440000". This format includes hyphens to separate
 * different parts of the UUID, enhancing human readability.
 *
 * This class provides utility functions for:
 *   - Generating new random UUIDs.
 *   - Creating UUIDs from given 128 bits.
 *   - Parsing UUIDs from and formatting them to their string representations.
 *   - Converting UUIDs to and from arrays of bytes.
 *   - Comparing UUIDs to establish ordering or equality.
 *
 * UUIDs are typically used in:
 *   - Generating unique identifiers for new database records.
 *   - Identifying objects in distributed systems without collision.
 *   - Serving as part of the URLs for identifying individual resources in web applications.
 *
 * @sample samples.uuid.UUIDs.parse
 * @sample samples.uuid.UUIDs.fromByteArray
 * @sample samples.uuid.UUIDs.random
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
     * For example, to retrieve the [version number](https://www.rfc-editor.org/rfc/rfc9562.html#section-4.2)
     * of this UUID:
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
     * For example, to identify whether a UUID is of the
     * [IETF variant (variant 2)](https://www.rfc-editor.org/rfc/rfc9562.html#section-4.1):
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
     * Returns the standard string representation of this UUID.
     *
     * The resulting string is in the format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
     * where 'x' represents a hexadecimal digit, also known as "hex-and-dash" format.
     * It is in lowercase and consists of 36 characters. Each hexadecimal digit
     * in the string sequentially represents the next 4 bits of the UUID, starting from the most
     * significant 4 bits in the first digit to the least significant 4 bits in the last digit.
     *
     * This format is the standard textual representation of UUIDs and is compatible with
     * UUID parsing logic found in most software environments. It is specified by
     * [RFC 9562 section 4](https://www.rfc-editor.org/rfc/rfc9562.html#section-4).
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
     * The resulting string is in lowercase and consists of 32 characters. Each hexadecimal digit
     * in the string sequentially represents the next 4 bits of the UUID, starting from the most
     * significant 4 bits in the first digit to the least significant 4 bits in the last digit.
     *
     * The returned string is equivalent to:
     * ```kotlin
     * uuid.toByteArray().toHexString()
     * ```
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
     * @return `true` if [other] is an instance of UUID, and consists of the same sequence
     *   of bits as this UUID; `false` otherwise.
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
         * This UUID can be used as a special value, for instance, as a placeholder for a
         * non-null but not yet initialized variable.
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
            if (mostSignificantBits == 0L && leastSignificantBits == 0L) {
                NIL
            } else {
                UUID(mostSignificantBits, leastSignificantBits)
            }

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
            fromLongs(mostSignificantBits.toLong(), leastSignificantBits.toLong())

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

            return fromLongs(byteArray.toLong(startIndex = 0), byteArray.toLong(startIndex = 8))
        }

        /**
         * Parses a UUID from the standard string representation as described in [UUID.toString].
         *
         * This function is case-insensitive, and for a valid [uuidString], the following property holds:
         * ```kotlin
         * val uuid = UUID.parse(uuidString)
         * assertEquals(uuid.toString(), uuidString.lowercase())
         * ```
         *
         * The standard textual representation of UUIDs is specified by
         * [RFC 9562 section 4](https://www.rfc-editor.org/rfc/rfc9562.html#section-4).
         *
         * @param uuidString A string in the format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
         *   where each 'x' is a hexadecimal digit, either lowercase or uppercase.
         * @throws IllegalArgumentException If the [uuidString] is not a 36-character string
         *   in the standard UUID format.
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
            return fromLongs(msb, lsb)
        }

        /**
         * Parses a UUID from the hexadecimal string representation as described in [UUID.toHexString].
         *
         * This function is case-insensitive, and for a valid [hexString], the following property holds:
         * ```kotlin
         * val uuid = UUID.parseHex(hexString)
         * assertEquals(uuid.toHexString(), hexString.lowercase())
         * ```
         *
         * @param hexString A 32-character hexadecimal string representing the UUID, without hyphens.
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
            return fromLongs(msb, lsb)
        }

        /**
         * Generates a new random UUID instance.
         *
         * The returned UUID conforms to [IETF variant (variant 2)](https://www.rfc-editor.org/rfc/rfc9562.html#section-4.1)
         * and [version 4](https://www.rfc-editor.org/rfc/rfc9562.html#section-4.2),
         * designed to be unique with a very high probability regardless of when or where it is generated.
         * The UUID is produced using a cryptographically secure pseudorandom number generator (CSPRNG)
         * available on the platform. If the underlying system has not collected enough entropy, this function
         * may block until sufficient entropy is collected and the CSPRNG is fully initialized.
         *
         * Note that the returned UUID is not recommended for use for cryptographic purposes.
         * Because version 4 UUID has a partially predictable bit pattern, and utilizes at most
         * 122 bits of entropy, regardless of platform. Additionally, on platforms that do not provide
         * a CSPRNG, the subsequent calls of this function could return guessable UUIDs.
         *
         * The following APIs are used for producing the random UUID in each of the supported targets:
         *   - Kotlin/JVM - [java.security.SecureRandom](https://docs.oracle.com/javase%2F8%2Fdocs%2Fapi%2F%2F/java/security/SecureRandom.html)
         *   - Kotlin/JS - [Crypto.getRandomBytes()](https://developer.mozilla.org/en-US/docs/Web/API/Crypto/getRandomValues)
         *   - Kotlin/WasmJs - [Crypto.getRandomBytes()](https://developer.mozilla.org/en-US/docs/Web/API/Crypto/getRandomValues)
         *   - Kotlin/WasmWasi - [random_get](https://github.com/WebAssembly/WASI/blob/main/legacy/preview1/docs.md#random_get)
         *   - Kotlin/Native:
         *       - Linux targets - [getrandom](https://www.man7.org/linux/man-pages/man2/getrandom.2.html)
         *       - Apple and Android Native targets - [arc4random_buf](https://man7.org/linux/man-pages/man3/arc4random_buf.3.html)
         *       - Windows targets - [BCryptGenRandom](https://learn.microsoft.com/en-us/windows/win32/api/bcrypt/nf-bcrypt-bcryptgenrandom)
         *
         * Note that the underlying API used to produce random UUIDs may change in the future.
         *
         * @return A randomly generated UUID.
         *
         * @sample samples.uuid.UUIDs.random
         */
        public fun random(): UUID =
            secureRandomUuid()

        /**
         * A Comparator that lexically orders UUIDs.
         *
         * This comparator compares the given two 128-bit UUIDs bit by bit sequentially,
         * starting from the most significant bit to the least significant.
         * UUID `a` is considered less than `b` if, at the first position where corresponding bits
         * of the two UUIDs differ, the bit in `a` is zero and the bit in `b` is one.
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
internal expect fun secureRandomUuid(): UUID

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
