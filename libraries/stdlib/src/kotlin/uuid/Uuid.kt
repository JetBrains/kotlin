/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("UuidKt")

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
 *   - Generating UUIDs.
 *   - Creating UUIDs from given 128 bits.
 *   - Parsing UUIDs from and formatting them to their string representations.
 *   - Converting UUIDs to and from arrays of bytes.
 *   - Comparing UUIDs to establish ordering or equality.
 *
 * @sample samples.uuid.Uuids.parse
 * @sample samples.uuid.Uuids.fromByteArray
 * @sample samples.uuid.Uuids.random
 */
@SinceKotlin("2.0")
@ExperimentalUuidApi
public class Uuid internal constructor(
    @PublishedApi internal val mostSignificantBits: Long,
    @PublishedApi internal val leastSignificantBits: Long
) : Serializable {

    /**
     * Executes the specified block of code, providing access to the uuid's bits in the form of two [Long] values.
     *
     * This function is intended for use when one needs to perform bitwise operations with the uuid.
     * For example, to retrieve the [version number](https://www.rfc-editor.org/rfc/rfc9562.html#section-4.2)
     * of this uuid:
     * ```kotlin
     * val version = uuid.toLongs { mostSignificantBits, _ ->
     *     ((mostSignificantBits shr 12) and 0xF).toInt()
     * }
     * ```
     *
     * The [action] will receive two [Long] arguments:
     *   - `mostSignificantBits`: The most significant 64 bits of this uuid presented in big-endian byte order.
     *   - `leastSignificantBits`: The least significant 64 bits of this uuid presented in big-endian byte order.
     *
     * For example, for the uuid `550e8400-e29b-41d4-a716-446655440000`, the breakdown is the following:
     *   - `mostSignificantBits = 0x550e8400e29b41d4L`.
     *   - `leastSignificantBits = 0xa716446655440000uL.toLong()`.
     *
     * @param action A function that takes two [Long] arguments (mostSignificantBits, leastSignificantBits).
     *   This function is guaranteed to be called exactly once.
     * @return The result of [action].
     *
     * @see Uuid.fromLongs
     */
    @InlineOnly
    public inline fun <T> toLongs(action: (mostSignificantBits: Long, leastSignificantBits: Long) -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }
        return action(mostSignificantBits, leastSignificantBits)
    }

    /**
     * Executes a specified block of code, providing access to the uuid's bits in the form of two [ULong] values.
     *
     * This function is intended for use when one needs to perform bitwise operations with the uuid.
     * For example, to identify whether this uuid is of the
     * [IETF variant (variant 2)](https://www.rfc-editor.org/rfc/rfc9562.html#section-4.1):
     * ```kotlin
     * val isIetfVariant = uuid.toULongs { _, leastSignificantBits ->
     *     (leastSignificantBits shr 62) == 2uL
     * }
     * ```
     *
     * The [action] will receive two [ULong] arguments:
     *   - `mostSignificantBits`: The most significant 64 bits of this uuid presented in big-endian byte order.
     *   - `leastSignificantBits`: The least significant 64 bits of this uuid presented in big-endian byte order.
     *
     * For example, for the uuid `550e8400-e29b-41d4-a716-446655440000`, the breakdown is the following:
     *   - `mostSignificantBits = 0x550e8400e29b41d4uL`.
     *   - `leastSignificantBits = 0xa716446655440000uL`.
     *
     * @param action A function that takes two [ULong] arguments (mostSignificantBits, leastSignificantBits).
     *   This function is guaranteed to be called exactly once.
     * @return The result of [action].
     *
     * @see Uuid.fromULongs
     */
    @InlineOnly
    public inline fun <T> toULongs(action: (mostSignificantBits: ULong, leastSignificantBits: ULong) -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }
        return action(mostSignificantBits.toULong(), leastSignificantBits.toULong())
    }

    /**
     * Returns the standard string representation of this uuid.
     *
     * The resulting string is in the format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
     * where 'x' represents a hexadecimal digit, also known as "hex-and-dash" format.
     * It is in lowercase and consists of 36 characters. Each hexadecimal digit
     * in the string sequentially represents the next 4 bits of the uuid, starting from the most
     * significant 4 bits in the first digit to the least significant 4 bits in the last digit.
     *
     * This format is the standard textual representation of uuids and is compatible with
     * uuid parsing logic found in most software environments. It is specified by
     * [RFC 9562 section 4](https://www.rfc-editor.org/rfc/rfc9562.html#section-4).
     *
     * @see Uuid.parse
     * @sample samples.uuid.Uuids.toStringSample
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
     * Returns the hexadecimal string representation of this uuid without hyphens.
     *
     * The resulting string is in lowercase and consists of 32 characters. Each hexadecimal digit
     * in the string sequentially represents the next 4 bits of the uuid, starting from the most
     * significant 4 bits in the first digit to the least significant 4 bits in the last digit.
     *
     * The returned string is equivalent to:
     * ```kotlin
     * uuid.toByteArray().toHexString()
     * ```
     *
     * @see Uuid.parseHex
     * @sample samples.uuid.Uuids.toHexString
     */
    public fun toHexString(): String {
        val bytes = ByteArray(32)
        leastSignificantBits.formatBytesInto(bytes, 16, 8)
        mostSignificantBits.formatBytesInto(bytes, 0, 8)
        return bytes.decodeToString()
    }

    /**
     * Returns a byte array representation of this uuid.
     *
     * The returned array contains 16 bytes. Each byte in the array sequentially represents
     * the next 8 bits of the uuid, starting from the most significant 8 bits
     * in the first byte to the least significant 8 bits in the last byte.
     *
     * @see Uuid.fromByteArray
     * @sample samples.uuid.Uuids.toByteArray
     */
    public fun toByteArray(): ByteArray {
        val bytes = ByteArray(SIZE_BYTES)
        mostSignificantBits.toByteArray(bytes, 0)
        leastSignificantBits.toByteArray(bytes, 8)
        return bytes
    }

    /**
     * Checks whether this uuid is equal to the specified [other] object.
     *
     * @param other The object to compare with this uuid.
     * @return `true` if [other] is an instance of [Uuid], and consists of the same sequence
     *   of bits as this uuid; `false` otherwise.
     *
     * @sample samples.uuid.Uuids.uuidEquals
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Uuid) return false
        return mostSignificantBits == other.mostSignificantBits &&
                leastSignificantBits == other.leastSignificantBits
    }

    override fun hashCode(): Int {
        val x = mostSignificantBits xor leastSignificantBits
        return (x shr 32).toInt() xor x.toInt()
    }

    public companion object {
        /**
         * The uuid with all bits set to zero.
         *
         * This uuid can be used as a special value, for instance, as a placeholder for a
         * non-null but not yet initialized variable.
         */
        public val NIL: Uuid = Uuid(0, 0)

        /** The number of bytes used to represent an instance of [Uuid] in a binary form. */
        public const val SIZE_BYTES: Int = 16

        /** The number of bits used to represent an instance of [Uuid] in a binary form. */
        public const val SIZE_BITS: Int = 128

        /**
         * Creates a uuid from specified 128 bits split into two 64-bit Longs.
         *
         * This function interprets the provided `Long` values in big-endian byte order.
         *
         * @param mostSignificantBits The most significant 64 bits of the uuid.
         * @param leastSignificantBits The least significant 64 bits of the uuid.
         * @return A new uuid based on the specified bits.
         *
         * @see Uuid.toLongs
         * @sample samples.uuid.Uuids.fromLongs
         */
        public fun fromLongs(mostSignificantBits: Long, leastSignificantBits: Long): Uuid =
            if (mostSignificantBits == 0L && leastSignificantBits == 0L) {
                NIL
            } else {
                Uuid(mostSignificantBits, leastSignificantBits)
            }

        /**
         * Creates a uuid from specified 128 bits split into two 64-bit ULongs.
         *
         * This function interprets the provided `ULong` values in big-endian byte order.
         *
         * @param mostSignificantBits The most significant 64 bits of the uuid.
         * @param leastSignificantBits The least significant 64 bits of the uuid.
         * @return A new uuid based on the specified bits.
         *
         * @see Uuid.toULongs
         * @sample samples.uuid.Uuids.fromULongs
         */
        public fun fromULongs(mostSignificantBits: ULong, leastSignificantBits: ULong): Uuid =
            fromLongs(mostSignificantBits.toLong(), leastSignificantBits.toLong())

        /**
         * Creates a uuid from a byte array containing 128 bits split into 16 bytes.
         *
         * Each byte in the [byteArray] sequentially represents
         * the next 8 bits of the uuid, starting from the most significant 8 bits
         * in the first byte to the least significant 8 bits in the last byte.
         *
         * @param byteArray A 16-byte array containing the uuid bits.
         * @throws IllegalArgumentException If the size of the [byteArray] is not exactly 16.
         * @return A new uuid based on the specified bits.
         *
         * @see Uuid.toByteArray
         * @sample samples.uuid.Uuids.fromByteArray
         */
        public fun fromByteArray(byteArray: ByteArray): Uuid {
            require(byteArray.size == SIZE_BYTES) { "Expected exactly $SIZE_BYTES bytes" }

            return fromLongs(byteArray.toLong(startIndex = 0), byteArray.toLong(startIndex = 8))
        }

        /**
         * Parses a uuid from the standard string representation as described in [Uuid.toString].
         *
         * This function is case-insensitive, and for a valid [uuidString], the following property holds:
         * ```kotlin
         * val uuid = Uuid.parse(uuidString)
         * assertEquals(uuid.toString(), uuidString.lowercase())
         * ```
         *
         * The standard textual representation of uuids is specified by
         * [RFC 9562 section 4](https://www.rfc-editor.org/rfc/rfc9562.html#section-4).
         *
         * @param uuidString A string in the format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
         *   where each 'x' is a hexadecimal digit, either lowercase or uppercase.
         * @throws IllegalArgumentException If the [uuidString] is not a 36-character string
         *   in the standard uuid format.
         * @return A uuid equivalent to the specified uuid string.
         *
         * @see Uuid.toString
         * @sample samples.uuid.Uuids.parse
         */
        @OptIn(ExperimentalStdlibApi::class)
        public fun parse(uuidString: String): Uuid {
            require(uuidString.length == 36) { "Expected a 36-char string in the standard uuid format." }

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
         * Parses a uuid from the hexadecimal string representation as described in [Uuid.toHexString].
         *
         * This function is case-insensitive, and for a valid [hexString], the following property holds:
         * ```kotlin
         * val uuid = Uuid.parseHex(hexString)
         * assertEquals(uuid.toHexString(), hexString.lowercase())
         * ```
         *
         * @param hexString A 32-character hexadecimal string representing the uuid, without hyphens.
         * @throws IllegalArgumentException If the [hexString] is not a 32-character hexadecimal string.
         * @return A uuid represented by the specified hexadecimal string.
         *
         * @see Uuid.toHexString
         * @sample samples.uuid.Uuids.parseHex
         */
        @OptIn(ExperimentalStdlibApi::class)
        public fun parseHex(hexString: String): Uuid {
            require(hexString.length == 32) { "Expected a 32-char hexadecimal string." }

            val msb = hexString.hexToLong(startIndex = 0, endIndex = 16)
            val lsb = hexString.hexToLong(startIndex = 16, endIndex = 32)
            return fromLongs(msb, lsb)
        }

        /**
         * Generates a new random [Uuid] instance.
         *
         * The returned uuid conforms to the [IETF variant (variant 2)](https://www.rfc-editor.org/rfc/rfc9562.html#section-4.1)
         * and [version 4](https://www.rfc-editor.org/rfc/rfc9562.html#section-4.2),
         * designed to be unique with a very high probability, regardless of when or where it is generated.
         * The uuid is produced using a cryptographically secure pseudorandom number generator (CSPRNG)
         * available on the platform. If the underlying system has not collected enough entropy, this function
         * may block until sufficient entropy is collected, and the CSPRNG is fully initialized. It is worth mentioning
         * that the PRNG used in the Kotlin/WasmWasi target is not guaranteed to be cryptographically secure.
         * See the list below for details about the API used for producing the random uuid in each supported target.
         *
         * Note that the returned uuid is not recommended for use for cryptographic purposes.
         * Because version 4 uuid has a partially predictable bit pattern, and utilizes at most
         * 122 bits of entropy, regardless of platform.
         *
         * The following APIs are used for producing the random uuid in each of the supported targets:
         *   - Kotlin/JVM - [java.security.SecureRandom](https://docs.oracle.com/javase/8/docs/api/java/security/SecureRandom.html)
         *   - Kotlin/JS - [Crypto.getRandomValues()](https://developer.mozilla.org/en-US/docs/Web/API/Crypto/getRandomValues)
         *   - Kotlin/WasmJs - [Crypto.randomUUID()](https://developer.mozilla.org/en-US/docs/Web/API/Crypto/randomUUID)
         *   - Kotlin/WasmWasi - [random_get](https://github.com/WebAssembly/WASI/blob/main/legacy/preview1/docs.md#random_get)
         *   - Kotlin/Native:
         *       - Linux targets - [getrandom](https://www.man7.org/linux/man-pages/man2/getrandom.2.html)
         *       - Apple and Android Native targets - [arc4random_buf](https://man7.org/linux/man-pages/man3/arc4random_buf.3.html)
         *       - Windows targets - [BCryptGenRandom](https://learn.microsoft.com/en-us/windows/win32/api/bcrypt/nf-bcrypt-bcryptgenrandom)
         *
         * Note that the underlying API used to produce random uuids may change in the future.
         *
         * @return A randomly generated uuid.
         * @throws RuntimeException if the underlying API fails. Refer to the corresponding underlying API
         *   documentation for possible reasons for failure and guidance on how to handle them.
         *
         * @sample samples.uuid.Uuids.random
         */
        public fun random(): Uuid =
            secureRandomUuid()

        /**
         * A Comparator that lexically orders uuids.
         *
         * This comparator compares the given two 128-bit uuids bit by bit sequentially,
         * starting from the most significant bit to the least significant.
         * uuid `a` is considered less than `b` if, at the first position where corresponding bits
         * of the two uuids differ, the bit in `a` is zero and the bit in `b` is one.
         * Conversely, `a` is considered greater than `b` if, at the first differing position,
         * the bit in `a` is one and the bit in `b` is zero.
         * If no differing bits are found, the two uuids are considered equal.
         *
         * The result of the comparison of uuids `a` and `b` by this comparator is equivalent to:
         * ```kotlin
         * a.toString().compareTo(b.toString())
         * ```
         *
         * @sample samples.uuid.Uuids.lexicalOrder
         */
        public val LEXICAL_ORDER: Comparator<Uuid> = Comparator { a, b ->
            if (a.mostSignificantBits != b.mostSignificantBits)
                a.mostSignificantBits.toULong().compareTo(b.mostSignificantBits.toULong())
            else
                a.leastSignificantBits.toULong().compareTo(b.leastSignificantBits.toULong())
        }
    }
}

@ExperimentalUuidApi
internal expect fun secureRandomUuid(): Uuid

@ExperimentalUuidApi
internal fun uuidFromRandomBytes(randomBytes: ByteArray): Uuid {
    randomBytes[6] = (randomBytes[6].toInt() and 0x0f).toByte() /* clear version        */
    randomBytes[6] = (randomBytes[6].toInt() or 0x40).toByte()  /* set to version 4     */
    randomBytes[8] = (randomBytes[8].toInt() and 0x3f).toByte() /* clear variant        */
    randomBytes[8] = (randomBytes[8].toInt() or 0x80).toByte()  /* set to IETF variant  */
    return Uuid.fromByteArray(randomBytes)
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

@OptIn(ExperimentalStdlibApi::class)
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
    require(this[index] == '-') { "Expected '-' (hyphen) at index $index, but was '${this[index]}'" }
}

private fun Long.toByteArray(dst: ByteArray, dstOffset: Int) {
    for (index in 0 until 8) {
        val shift = 8 * (7 - index)
        dst[dstOffset + index] = (this ushr shift).toByte()
    }
}
