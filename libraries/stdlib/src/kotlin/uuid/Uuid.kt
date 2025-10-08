/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("UuidKt")

package kotlin.uuid

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.internal.InlineOnly
import kotlin.internal.ReadObjectParameterType
import kotlin.internal.throwReadObjectNotSupported
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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
public class Uuid private constructor(
    @PublishedApi internal val mostSignificantBits: Long,
    @PublishedApi internal val leastSignificantBits: Long,
) : Comparable<Uuid>, Serializable {

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
     * This function returns the same value as [toHexDashString].
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
     * @see Uuid.toHexDashString
     * @sample samples.uuid.Uuids.toStringSample
     */
    override fun toString(): String {
        return toHexDashString()
    }

    /**
     * Returns the standard hex-and-dash string representation of this uuid.
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
     * @see Uuid.parseHexDash
     * @sample samples.uuid.Uuids.toHexDashString
     */
    @SinceKotlin("2.1")
    public fun toHexDashString(): String {
        val bytes = ByteArray(UUID_HEX_DASH_LENGTH)
        mostSignificantBits.formatBytesInto(bytes, 0, startIndex = 0, endIndex = 4)
        bytes[8] = '-'.code.toByte()
        mostSignificantBits.formatBytesInto(bytes, 9, startIndex = 4, endIndex = 6)
        bytes[13] = '-'.code.toByte()
        mostSignificantBits.formatBytesInto(bytes, 14, startIndex = 6, endIndex = 8)
        bytes[18] = '-'.code.toByte()
        leastSignificantBits.formatBytesInto(bytes, 19, startIndex = 0, endIndex = 2)
        bytes[23] = '-'.code.toByte()
        leastSignificantBits.formatBytesInto(bytes, 24, startIndex = 2, endIndex = 8)
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
        val bytes = ByteArray(UUID_HEX_LENGTH)
        mostSignificantBits.formatBytesInto(bytes, 0, startIndex = 0, endIndex = 8)
        leastSignificantBits.formatBytesInto(bytes, 16, startIndex = 0, endIndex = 8)
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
        bytes.setLongAt(0, mostSignificantBits)
        bytes.setLongAt(8, leastSignificantBits)
        return bytes
    }

    /**
     * Returns an unsigned byte array representation of this uuid.
     *
     * The returned array contains 16 unsigned bytes. Each byte in the array sequentially represents
     * the next 8 bits of the uuid, starting from the most significant 8 bits
     * in the first byte to the least significant 8 bits in the last byte.
     *
     * @see Uuid.fromUByteArray
     * @sample samples.uuid.Uuids.toUByteArray
     */
    @SinceKotlin("2.1")
    @ExperimentalUnsignedTypes
    public fun toUByteArray(): UByteArray {
        return UByteArray(storage = toByteArray())
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

    /**
     * Compares this uuid with the [other] uuid for lexical order.
     *
     * Returns zero if this uuid is lexically equal to the specified other uuid, a negative number
     * if it's less than the other, or a positive number if it's greater than the other.
     *
     * This function compares the two 128-bit uuids bit by bit sequentially,
     * starting from the most significant bit to the least significant.
     * `this` uuid is considered less than `other` if, at the first position where corresponding bits
     * of the two uuids differ, the bit in `this` is zero and the bit in `other` is one.
     * Conversely, `this` is considered greater than `other` if, at the first differing position,
     * the bit in `this` is one and the bit in `other` is zero.
     * If no differing bits are found, the two uuids are considered equal.
     *
     * The result of the comparison of `this` and `other` uuids is equivalent to:
     * ```kotlin
     * this.toString().compareTo(other.toString())
     * ```
     *
     * @sample samples.uuid.Uuids.compareTo
     */
    @SinceKotlin("2.1")
    override fun compareTo(other: Uuid): Int {
        return if (mostSignificantBits != other.mostSignificantBits)
            mostSignificantBits.toULong().compareTo(other.mostSignificantBits.toULong())
        else
            leastSignificantBits.toULong().compareTo(other.leastSignificantBits.toULong())
    }

    override fun hashCode(): Int {
        return (mostSignificantBits xor leastSignificantBits).hashCode()
    }

    private fun writeReplace(): Any = serializedUuid(this)

    private fun readObject(input: ReadObjectParameterType): Unit = throwReadObjectNotSupported()

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
            require(byteArray.size == SIZE_BYTES) {
                "Expected exactly $SIZE_BYTES bytes, but was ${byteArray.truncateForErrorMessage(32)} of size ${byteArray.size}"
            }

            return fromLongs(byteArray.getLongAt(index = 0), byteArray.getLongAt(index = 8))
        }

        /**
         * Creates a uuid from an array containing 128 bits split into 16 unsigned bytes.
         *
         * Each unsigned byte in the [ubyteArray] sequentially represents
         * the next 8 bits of the uuid, starting from the most significant 8 bits
         * in the first byte to the least significant 8 bits in the last byte.
         *
         * @param ubyteArray A 16-byte array containing the uuid bits.
         * @throws IllegalArgumentException If the size of the [ubyteArray] is not exactly 16.
         * @return A new uuid based on the specified bits.
         *
         * @see Uuid.toUByteArray
         * @sample samples.uuid.Uuids.fromUByteArray
         */
        @SinceKotlin("2.1")
        @ExperimentalUnsignedTypes
        public fun fromUByteArray(ubyteArray: UByteArray): Uuid {
            return fromByteArray(ubyteArray.storage)
        }

        /**
         * Parses a uuid from one of the supported string representations.
         *
         * This function supports parsing the standard hex-and-dash and the hexadecimal string representations.
         * For details about the hex-and-dash format, refer to [toHexDashString].
         * If parsing only the hex-and-dash format is desired, use [parseHexDash] instead.
         * For details about the hexadecimal format, refer to [toHexString].
         * If parsing only the hexadecimal format is desired, use [parseHex] instead.
         *
         * Note that this function is case-insensitive,
         * meaning both lowercase and uppercase hexadecimal digits are considered valid.
         * Additionally, support for more uuid formats may be introduced in the future.
         * Therefore, users should not rely on the rejection of formats not currently supported.
         *
         * @param uuidString A string in one of the supported uuid formats.
         * @throws IllegalArgumentException If the [uuidString] is not in a supported uuid format.
         * @return A uuid equivalent to the specified uuid string.
         *
         * @see Uuid.parseHexDash
         * @see Uuid.parseHex
         * @sample samples.uuid.Uuids.parse
         */
        public fun parse(uuidString: String): Uuid {
            return when (uuidString.length) {
                UUID_HEX_DASH_LENGTH -> uuidParseHexDash(uuidString)
                UUID_HEX_LENGTH -> uuidParseHex(uuidString)
                else -> throw IllegalArgumentException(
                    "Expected either a 36-char string in the standard hex-and-dash UUID format or a 32-char hexadecimal string, " +
                            "but was \"${uuidString.truncateForErrorMessage(64)}\" of length ${uuidString.length}"
                )
            }
        }

        /**
         * Parses a uuid from one of the supported string representations, returning `null` if it matches none of them.
         *
         * This function supports parsing the standard hex-and-dash and the hexadecimal string representations.
         * For details about the hex-and-dash format, refer to [toHexDashString].
         * If parsing only the hex-and-dash format is desired, use [parseHexDashOrNull] instead.
         * For details about the hexadecimal format, refer to [toHexString].
         * If parsing only the hexadecimal format is desired, use [parseHexOrNull] instead.
         *
         * Note that this function is case-insensitive,
         * meaning both lowercase and uppercase hexadecimal digits are considered valid.
         * Additionally, support for more uuid formats may be introduced in the future.
         * Therefore, users should not rely on the rejection of formats not currently supported.
         *
         * @param uuidString A string in one of the supported uuid formats.
         * @return A uuid equivalent to the specified uuid string, or `null`,
         * if the string does not conform any of supported formats.
         *
         * @see Uuid.parseHexDashOrNull
         * @see Uuid.parseHexOrNull
         * @see Uuid.parse
         * @sample samples.uuid.Uuids.parseOrNull
         */
        @SinceKotlin("2.3")
        public fun parseOrNull(uuidString: String): Uuid? {
            return when (uuidString.length) {
                UUID_HEX_DASH_LENGTH -> parseHexDashOrNull(uuidString)
                UUID_HEX_LENGTH -> parseHexOrNull(uuidString)
                else -> null
            }
        }

        /**
         * Parses a uuid from the standard hex-and-dash string representation as described in [Uuid.toHexDashString].
         *
         * This function is case-insensitive, and for a valid [hexDashString], the following property holds:
         * ```kotlin
         * val uuid = Uuid.parseHexDash(hexDashString)
         * assertEquals(uuid.toHexDashString(), hexDashString.lowercase())
         * ```
         *
         * The standard textual representation of uuids, also known as hex-and-dash format, is specified by
         * [RFC 9562 section 4](https://www.rfc-editor.org/rfc/rfc9562.html#section-4).
         *
         * @param hexDashString A string in the format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
         *   where each 'x' is a hexadecimal digit, either lowercase or uppercase.
         * @throws IllegalArgumentException If the [hexDashString] is not a 36-character string
         *   in the standard uuid format.
         * @return A uuid equivalent to the specified uuid string.
         *
         * @see Uuid.toHexDashString
         * @see Uuid.parseHexDashOrNull
         * @sample samples.uuid.Uuids.parseHexDash
         */
        @SinceKotlin("2.1")
        public fun parseHexDash(hexDashString: String): Uuid {
            require(hexDashString.length == UUID_HEX_DASH_LENGTH) {
                "Expected a 36-char string in the standard hex-and-dash UUID format, " +
                        "but was \"${hexDashString.truncateForErrorMessage(64)}\" of length ${hexDashString.length}"
            }
            return uuidParseHexDash(hexDashString)
        }

        /**
         * Parses a uuid from the standard hex-and-dash string representation as described in [Uuid.toHexDashString],
         * returning `null` is a string has a different format.
         *
         * This function is case-insensitive, and for a valid [hexDashString], the following property holds:
         * ```kotlin
         * val uuid = Uuid.parseHexDashOrNull(hexDashString)!!
         * assertEquals(uuid.toHexDashString(), hexDashString.lowercase())
         * ```
         *
         * The standard textual representation of uuids, also known as hex-and-dash format, is specified by
         * [RFC 9562 section 4](https://www.rfc-editor.org/rfc/rfc9562.html#section-4).
         *
         * @param hexDashString A string in the format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
         *   where each 'x' is a hexadecimal digit, either lowercase or uppercase.
         * @return A uuid equivalent to the specified uuid string, or `null`, if the string does not conform the format.
         *
         * @see Uuid.toHexDashString
         * @see Uuid.parseHexDash
         * @sample samples.uuid.Uuids.parseHexDashOrNull
         */
        @SinceKotlin("2.3")
        public fun parseHexDashOrNull(hexDashString: String): Uuid? {
            if (hexDashString.length != UUID_HEX_DASH_LENGTH) return null
            return uuidParseHexDashOrNull(hexDashString)
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
         * @see Uuid.parseHexOrNull
         * @sample samples.uuid.Uuids.parseHex
         */
        public fun parseHex(hexString: String): Uuid {
            require(hexString.length == UUID_HEX_LENGTH) {
                "Expected a 32-char hexadecimal string, " +
                        "but was \"${hexString.truncateForErrorMessage(64)}\" of length ${hexString.length}"
            }
            return uuidParseHex(hexString)
        }

        /**
         * Parses a uuid from the hexadecimal string representation as described in [Uuid.toHexString],
         * returning `null` if a string has a different format.
         *
         * This function is case-insensitive, and for a valid [hexString], the following property holds:
         * ```kotlin
         * val uuid = Uuid.parseHexOrNull(hexString)!!
         * assertEquals(uuid.toHexString(), hexString.lowercase())
         * ```
         *
         * @param hexString A 32-character hexadecimal string representing the uuid, without hyphens.
         * @return A uuid represented by the specified hexadecimal string, or `null`, if the string does not conform the format.
         *
         * @see Uuid.toHexString
         * @see Uuid.parseHex
         * @sample samples.uuid.Uuids.parseHexOrNull
         */
        @SinceKotlin("2.3")
        public fun parseHexOrNull(hexString: String): Uuid? {
            if (hexString.length != UUID_HEX_LENGTH) return null
            return uuidParseHexOrNull(hexString)
        }

        /**
         * Generates a new random [Uuid] instance.
         *
         * This function is synonymous to [Uuid.generateV4].
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
         *   - Kotlin/WasmJs - [Crypto.getRandomValues()](https://developer.mozilla.org/en-US/docs/Web/API/Crypto/getRandomValues)
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
         * @see Uuid.generateV4
         * @sample samples.uuid.Uuids.random
         */
        public fun random(): Uuid = generateV4()

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
         *   - Kotlin/WasmJs - [Crypto.getRandomValues()](https://developer.mozilla.org/en-US/docs/Web/API/Crypto/getRandomValues)
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
         * @sample samples.uuid.Uuids.v4
         */
        @SinceKotlin("2.3")
        public fun generateV4(): Uuid = secureRandomUuid()

        /**
         * Generates a new random [Uuid] Version 7 instance.
         *
         * The returned uuid is a time-based sortable UUID that conforms to the
         * [IETF variant (variant 2)](https://www.rfc-editor.org/rfc/rfc9562.html#section-4.1)
         * and [version 7](https://www.rfc-editor.org/rfc/rfc9562.html#section-4.2),
         * uses UNIX timestamp in milliseconds as a prefix and a randomly generated suffix,
         * allowing several consecutively generated uuids to be monotonically ordered and yet keep future uuid values unguessable.
         *
         * The current implementation guarantees strict monotonicity of returned uuids within the scope of an application lifetime.
         * There are no monotonicity guarantees for two uuids generated in separate processes on the same host,
         * as well as for uudis generated on different hosts.
         * If multiple uuids were requested at the exact same instant of time, the current implementation will use
         * the "Fixed Bit-Length Dedicated Counter" method covered by the
         * [RFC-9562, §6.2. Monotonicity and Counters](https://www.rfc-editor.org/rfc/rfc9562.html#section-6.2)
         * to achieve strict monotonicity.
         *
         * The random part of the uuid is produced using a cryptographically secure pseudorandom number generator (CSPRNG)
         * available on the platform.
         * If the underlying system has not collected enough entropy, this function
         * may block until sufficient entropy is collected, and the CSPRNG is fully initialized.
         * It is worth mentioning
         * that the PRNG used in the Kotlin/WasmWasi target is not guaranteed to be cryptographically secure.
         * See the list below for details about the API used for producing the random uuid in each supported target.
         *
         * Note that the returned uuid is not recommended for use for cryptographic purposes.
         * Because version 7 uuid has a partially predictable bit pattern, and utilizes at most
         * 74 bits of entropy, regardless of platform.
         *
         * The following APIs are used for producing the random uuid in each of the supported targets:
         *   - Kotlin/JVM - [java.security.SecureRandom](https://docs.oracle.com/javase/8/docs/api/java/security/SecureRandom.html)
         *   - Kotlin/JS - [Crypto.getRandomValues()](https://developer.mozilla.org/en-US/docs/Web/API/Crypto/getRandomValues)
         *   - Kotlin/WasmJs - [Crypto.getRandomValues()](https://developer.mozilla.org/en-US/docs/Web/API/Crypto/getRandomValues)
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
         * @sample samples.uuid.Uuids.v7
         */
        @SinceKotlin("2.3")
        @OptIn(ExperimentalTime::class)
        public fun generateV7(): Uuid = generateV7(Clock.System)

        @OptIn(ExperimentalTime::class)
        internal fun generateV7(clock: Clock): Uuid = UuidV7Generator.generate(clock)

        /**
         * A [Comparator] that lexically orders uuids.
         *
         * Note:
         *   [Uuid] is a [Comparable] type, and its [compareTo] function establishes lexical order.
         *   [LEXICAL_ORDER] was introduced when `Uuid`s were not comparable.
         *   It is now deprecated and will be removed in a future release.
         *   Instead, use [`naturalOrder<Uuid>()`][naturalOrder], which is equivalent to [LEXICAL_ORDER].
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
        @Deprecated("Use naturalOrder<Uuid>() instead", ReplaceWith("naturalOrder<Uuid>()", imports = ["kotlin.comparisons.naturalOrder"]))
        @DeprecatedSinceKotlin(warningSince = "2.1")
        public val LEXICAL_ORDER: Comparator<Uuid>
            get() = naturalOrder()
    }
}

private const val UUID_HEX_LENGTH = 32
private const val UUID_HEX_DASH_LENGTH = 36

@ExperimentalUuidApi
internal expect fun serializedUuid(uuid: Uuid): Any

@ExperimentalUuidApi
internal fun secureRandomUuid(): Uuid {
    return uuidFromRandomBytes(ByteArray(Uuid.SIZE_BYTES).also {
        secureRandomBytes(it)
    })
}

internal expect fun secureRandomBytes(destination: ByteArray): Unit

@ExperimentalUuidApi
internal fun uuidFromRandomBytes(randomBytes: ByteArray): Uuid {
    randomBytes[6] = (randomBytes[6].toInt() and 0x0f).toByte() /* clear version        */
    randomBytes[6] = (randomBytes[6].toInt() or 0x40).toByte()  /* set to version 4     */
    randomBytes[8] = (randomBytes[8].toInt() and 0x3f).toByte() /* clear variant        */
    randomBytes[8] = (randomBytes[8].toInt() or 0x80).toByte()  /* set to IETF variant  */
    return Uuid.fromByteArray(randomBytes)
}

/**
 * Extracts 8 bytes from this byte array starting at [index] to form a Long.
 * The extraction is performed in a big-endian manner.
 * Specifically, the byte at `index` becomes the highest byte,
 * and the byte at `index + 7` becomes the lowest byte.
 */
// Implement differently in JS to avoid bitwise operations with Longs
@ExperimentalUuidApi
internal expect fun ByteArray.getLongAt(index: Int): Long

internal fun ByteArray.getLongAtCommonImpl(index: Int): Long {
    return ((this[index + 0].toLong() and 0xFF) shl 56) or
            ((this[index + 1].toLong() and 0xFF) shl 48) or
            ((this[index + 2].toLong() and 0xFF) shl 40) or
            ((this[index + 3].toLong() and 0xFF) shl 32) or
            ((this[index + 4].toLong() and 0xFF) shl 24) or
            ((this[index + 5].toLong() and 0xFF) shl 16) or
            ((this[index + 6].toLong() and 0xFF) shl 8) or
            (this[index + 7].toLong() and 0xFF)
}

/**
 * Formats this Long as hexadecimal and stores the resulting hexadecimal digits into [dst].
 * Storage begins at [dstOffset] and proceeds forwards.
 * Formatting starts from the byte at [startIndex] and continues up to [endIndex] (exclusive).
 * The index of the highest byte in this Long is `0`, and the index of the lowest byte is `7`.
 */
// Implement differently in JS to avoid bitwise operations with Longs
@ExperimentalUuidApi
internal expect fun Long.formatBytesInto(dst: ByteArray, dstOffset: Int, startIndex: Int, endIndex: Int)

@ExperimentalUuidApi
internal fun Long.formatBytesIntoCommonImpl(dst: ByteArray, dstOffset: Int, startIndex: Int, endIndex: Int) {
    var dstIndex = dstOffset
    for (reversedIndex in 7 - startIndex downTo 8 - endIndex) {
        val shift = reversedIndex shl 3
        val byte = ((this shr shift) and 0xFF).toInt()
        val byteDigits = BYTE_TO_LOWER_CASE_HEX_DIGITS[byte]
        dst[dstIndex++] = (byteDigits shr 8).toByte()
        dst[dstIndex++] = byteDigits.toByte()
    }
}

/**
 * Extracts bytes from the Long [value] and stores them into this byte array starting at [index].
 * The bytes are stored in a big-endian manner.
 * Specifically, the highest byte of `value` is stored at `index`,
 * and the lowest byte is stored at `index + 7`.
 */
// Implement differently in JS to avoid bitwise operations with Longs
@ExperimentalUuidApi
internal expect fun ByteArray.setLongAt(index: Int, value: Long)

internal fun ByteArray.setLongAtCommonImpl(index: Int, value: Long) {
    var i = index
    for (reversedIndex in 7 downTo 0) {
        val shift = reversedIndex shl 3
        this[i++] = (value shr shift).toByte()
    }
}

// Implement differently in JS to avoid bitwise operations with Longs
@ExperimentalUuidApi
internal expect fun uuidParseHexDash(hexDashString: String): Uuid

// Implement differently in JS to avoid bitwise operations with Longs
@ExperimentalUuidApi
internal expect fun uuidParseHexDashOrNull(hexDashString: String): Uuid?

@ExperimentalUuidApi
internal fun uuidParseHexDashCommonImpl(hexDashString: String): Uuid {
    return uuidParseHexDashCommonImpl(hexDashString) { inputString, errorDescription, errorPosition ->
        uuidThrowUnexpectedCharacterException(inputString, errorDescription, errorPosition)
    }
}

@ExperimentalUuidApi
internal fun uuidParseHexDashOrNullCommonImpl(hexDashString: String): Uuid? {
    return uuidParseHexDashCommonImpl(hexDashString) { _, _, _ ->
        return null
    }
}

internal inline fun String.uuidCheckHyphenAt(
    index: Int,
    onError: (inputString: String, errorDescription: String, errorPosition: Int) -> Unit
) {
    if (this[index] != '-') onError(this, "'-' (hyphen)", index)
}

@ExperimentalUuidApi
internal inline fun uuidParseHexDashCommonImpl(
    hexDashString: String,
    onError: (inputString: String, errorDescription: String, errorPosition: Int) -> Nothing
): Uuid {
    val hexDigitExpectedMessage = "a hexadecimal digit"

    // xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
    // 16 hex digits fit into a Long
    val part1 = hexDashString.parseHexToLong(startIndex = 0, endIndex = 8) { onError(this, hexDigitExpectedMessage, it) }
    hexDashString.uuidCheckHyphenAt(8, onError)
    val part2 = hexDashString.parseHexToLong(startIndex = 9, endIndex = 13) { onError(this, hexDigitExpectedMessage, it) }
    hexDashString.uuidCheckHyphenAt(13, onError)
    val part3 = hexDashString.parseHexToLong(startIndex = 14, endIndex = 18) { onError(this, hexDigitExpectedMessage, it) }
    hexDashString.uuidCheckHyphenAt(18, onError)
    val part4 = hexDashString.parseHexToLong(startIndex = 19, endIndex = 23) { onError(this, hexDigitExpectedMessage, it) }
    hexDashString.uuidCheckHyphenAt(23, onError)
    val part5 = hexDashString.parseHexToLong(startIndex = 24, endIndex = 36) { onError(this, hexDigitExpectedMessage, it) }

    val msb = (part1 shl 32) or (part2 shl 16) or part3
    val lsb = (part4 shl 48) or part5
    return Uuid.fromLongs(msb, lsb)
}

// Implement differently in JS to avoid bitwise operations with Longs
@ExperimentalUuidApi
internal expect fun uuidParseHex(hexString: String): Uuid

@ExperimentalUuidApi
internal expect fun uuidParseHexOrNull(hexString: String): Uuid?

@ExperimentalUuidApi
internal fun uuidParseHexCommonImpl(hexString: String): Uuid {
    return uuidParseHexCommonImpl(hexString) { inputString, errorDescription, errorIndex ->
        uuidThrowUnexpectedCharacterException(inputString, errorDescription, errorIndex)
    }
}

@ExperimentalUuidApi
internal fun uuidParseHexOrNullCommonImpl(hexString: String): Uuid? {
    return uuidParseHexCommonImpl(hexString) { _, _, _ ->
        return null
    }
}

@ExperimentalUuidApi
internal inline fun uuidParseHexCommonImpl(
    hexString: String,
    onError: (inputString: String, errorDescription: String, errorIndex: Int) -> Nothing
): Uuid {
    // 16 hex digits fit into a Long
    val msb = hexString.parseHexToLong(startIndex = 0, endIndex = UUID_HEX_LENGTH / 2) { onError(this, "a hexadecimal digit", it) }
    val lsb = hexString.parseHexToLong(startIndex = UUID_HEX_LENGTH / 2, endIndex = UUID_HEX_LENGTH) { onError(this, "a hexadecimal digit", it) }
    return Uuid.fromLongs(msb, lsb)
}

private fun String.truncateForErrorMessage(maxLength: Int): String {
    return if (length <= maxLength) this else substring(0, maxLength) + "..."
}

private fun ByteArray.truncateForErrorMessage(maxSize: Int): String {
    return joinToString(prefix = "[", postfix = "]", limit = maxSize)
}

internal fun uuidThrowUnexpectedCharacterException(inputString: String, errorDescription: String, errorIndex: Int): Nothing {
    throw IllegalArgumentException("Expected $errorDescription at index $errorIndex, but was '${inputString[errorIndex]}'")
}

@OptIn(ExperimentalAtomicApi::class)
private object UuidV7Generator {
    private const val TIMESTAMP_BIAS_BITS = 16
    // covers ver and rand_a fields, set ver to 7
    private const val VERSION_MASK = 0x7000
    // if counter's highest bit is set to one, we have an overflow
    private const val OVERFLOW_MASK = 0x8000L

    // Stores both last used timestamp in millis and 12-bit wide counter,
    // both conveniently separated by 4-bit UUID version.
    //
    // Layout:                                          TIMESTAMP_BIAS_BITS
    //                                                   /
    // 64                                               16   12            0
    //  |----------unix timestamp in milliseconds--------|rdzn|--counter---|
    //  |tttttttttttttttttttttttttttttttttttttttttttttttt|0111|cccccccccccc|
    //
    // Where rdzn (or a red zone) works both as a valid UUID version for UUIDv7 (0b0111)
    // and works as an overflow guard.
    private val timestampAndCounter = AtomicLong(0L)

    /**
     * Generate a new Version 7 [Uuid] using [clock] as a timestamp source.
     *
     * Implementation uses a fixed bit-length dedicated counter occupying all 12 bits of rand_a field,
     * uses a fixed bit-length dedicated counter seeding to (re) initialize a counter and
     * tracks counter overflows. When re-initializing the counter, its most significant bit is always unset
     * to increase the values range.
     *
     * Refer to [RFC-9562, 6.2. Monotonicity and Counters](https://www.rfc-editor.org/rfc/rfc9562.html#section-6.2)
     * for more details.
     *
     * This implementation is thread safe.
     */
    @OptIn(ExperimentalTime::class)
    @ExperimentalUuidApi
    fun generate(clock: Clock): Uuid {
        // we need random values for:
        // - 62 bit random rand_b, which will be placed in the first 8 bytes
        // - 11 bit random rand_a, which will be placed in the trailing two bytes
        val randomBytes = ByteArray(10).also {
            secureRandomBytes(it)
        }

        // Let's keep moderate optimism and initialize re-initialize the counter beforehand.
        // Note that the MSB is always unset (thus the mask is 0x07).
        val newCounter = randomBytes[8].toInt().and(0x07).shl(8).or(
            randomBytes[9].toInt().and(0xFF)
        ).or(VERSION_MASK)

        var newTimeStampAndCounter: Long

        while (true) {
            val previousTimeStampAndCounter = timestampAndCounter.load()
            val currentTimeMillis = clock.now().toEpochMilliseconds()

            val previousTimeMillis = previousTimeStampAndCounter.ushr(TIMESTAMP_BIAS_BITS)

            if (previousTimeMillis < currentTimeMillis) { // clocks are ticking!
                // concatenate a new timestamp with a counter value
                newTimeStampAndCounter = currentTimeMillis.shl(TIMESTAMP_BIAS_BITS).or(newCounter.toLong())
                // try to save them, retry everything on failure
                if (timestampAndCounter.compareAndSet(previousTimeStampAndCounter, newTimeStampAndCounter)) {
                    break
                } // else -> continue
            } else { // clocks are not ticking :(
                // increment the counter
                newTimeStampAndCounter = previousTimeStampAndCounter + 1
                // check for the overflow
                if (newTimeStampAndCounter.and(OVERFLOW_MASK) != 0L) {
                    // counter overflow, let's increment timestamp by 1ms and reseed the counter
                    newTimeStampAndCounter = (previousTimeMillis + 1L).shl(TIMESTAMP_BIAS_BITS).or(newCounter.toLong())
                }
                // try to save updated timestamp and counter values, retry everything on failure
                if (timestampAndCounter.compareAndSet(previousTimeStampAndCounter, newTimeStampAndCounter)) {
                    break
                }
            }
        }

        // newTimeStampAndCounter is a valid Uuid prefix, se can just copy it:
        // - first (in the big-endian order) 48 bits are timestamp
        // - followed by version (0b0111)
        // - followed by 12 bit rand_a field
        //
        // For the suffix, we need to copy first 8 random bytes
        // and set two most significant bits to a valid variant value:
        // - variant (0b10)
        // - rand_b (62 bit)
        randomBytes[0] = randomBytes[0]
            .and(0x3F) // clear two MSBs
            .or(0x80.toByte()) // set then to 0b10
        val variantAndRandB = randomBytes.getLongAt(0)
        return Uuid.fromLongs(newTimeStampAndCounter, variantAndRandB)
    }
}
