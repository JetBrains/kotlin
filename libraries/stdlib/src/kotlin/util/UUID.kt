/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// TODO: decide on package
package kotlin

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.InlineOnly

/**
 * Represents a Universally Unique Identifier (UUID), also known as a Globally Unique Identifier (GUID).
 * A UUID is 128 bits long and is used to uniquely identify information across the globe.
 *
 * The standard textual representation of a UUID is "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", where each 'x'
 * is a hexadecimal digit. This class facilitates the creation, parsing, and management of UUIDs.
 */
public class UUID internal constructor(
    @PublishedApi internal val mostSignificantBits: Long,
    @PublishedApi internal val leastSignificantBits: Long
) {

    /**
     * Executes a specified block of code, providing access to the UUID's bits.
     * This function is intended for use when one needs to perform bitwise operations with the UUID.
     *
     * The [action] will receive two [Long] arguments:
     *   - `mostSignificantBits`: The most significant 64 bits of this UUID.
     *   - `leastSignificantBits`: The least significant 64 bits of this UUID.
     *
     * @param action A function that takes two [Long] arguments (mostSignificantBits, leastSignificantBits).
     *   This function is guaranteed to be called exactly once.
     */
    @InlineOnly
    public inline fun <T> toLongs(action: (mostSignificantBits: Long, leastSignificantBits: Long) -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }
        return action(mostSignificantBits, leastSignificantBits)
    }

    /**
     * Executes a specified block of code, providing access to the UUID's bits.
     * This function is intended for use when one needs to perform bitwise operations with the UUID.
     *
     * The [action] will receive two [ULong] arguments:
     *   - `mostSignificantBits`: The most significant 64 bits of this UUID.
     *   - `leastSignificantBits`: The least significant 64 bits of this UUID.
     *
     * @param action A function that takes two [ULong] arguments (mostSignificantBits, leastSignificantBits).
     *   This function is guaranteed to be called exactly once.
     */
    @InlineOnly
    public inline fun <T> toULongs(action: (mostSignificantBits: ULong, leastSignificantBits: ULong) -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }
        return action(mostSignificantBits.toULong(), leastSignificantBits.toULong())
    }

    /**
     * Returns the string representation of this UUID in the format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
     * where 'x' represents a hexadecimal digit. The string is in lowercase.
     */
    override fun toString(): String {
        return uuidToString(this)
    }

    /**
     * Returns the hexadecimal string representation of the UUID without hyphens.
     * The string is in lowercase.
     */
    public fun toHexString(): String {
        return uuidToHexString(this)
    }

    /**
     * Returns a byte array representation of this UUID.
     *
     * The array is 16 bytes long, representing the UUID in big-endian byte order.
     */
    public fun toByteArray(): ByteArray {
        return uuidToByteArray(this)
    }

    public companion object {
        /** The UUID with all bits set to zero. */
        public val NIL: UUID = UUID(0, 0)

        /** The number of bytes used to represent an instance of UUID in a binary form. */
        public const val SIZE_BYTES: Int = 16

        /** The number of bits used to represent an instance of UUID in a binary form. */
        public const val SIZE_BITS: Int = 128

        /**
         * Creates a UUID from specified 128 bits split into two 64-bit Longs.
         *
         * @param mostSignificantBits The most significant 64 bits.
         * @param leastSignificantBits The least significant 64 bits.
         * @return A new UUID based on the specified bits.
         */
        public fun fromLongs(mostSignificantBits: Long, leastSignificantBits: Long): UUID =
            UUID(mostSignificantBits, leastSignificantBits)

        /**
         * Creates a UUID from specified 128 bits split into two 64-bit ULongs.
         *
         * @param mostSignificantBits The most significant 64 bits.
         * @param leastSignificantBits The least significant 64 bits.
         * @return A new UUID based on the specified bits.
         */
        public fun fromULongs(mostSignificantBits: ULong, leastSignificantBits: ULong): UUID =
            UUID(mostSignificantBits.toLong(), leastSignificantBits.toLong())

        /**
         * Creates a UUID from specified 128 bits split into 16 big-endian bytes.
         *
         * @param byteArray A 16-byte array containing the UUID bits.
         * @throws IllegalArgumentException If the size of the [byteArray] is not 16.
         * @return A new UUID based on the specified bits.
         */
        public fun fromByteArray(byteArray: ByteArray): UUID =
            uuidFromBytes(byteArray)

        /**
         * Parses a UUID from a standard UUID string format.
         *
         * @param uuidString A string in the format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", where each 'x'
         *   is a hexadecimal digit, either lowercase or uppercase.
         * @throws IllegalArgumentException If the [uuidString] is not a 36-char string in the standard UUID format.
         * @return A UUID equivalent to the specified UUID string.
         */
        public fun parse(uuidString: String): UUID =
            uuidFromString(uuidString)

        /**
         * Parses a UUID from a hexadecimal UUID string without hyphens.
         * @param hexString A 32-char hexadecimal string representing the UUID.
         * @throws IllegalArgumentException If the [hexString] is not a 32-char hexadecimal string.
         * @return A UUID represented by the specified hexadecimal string.
         */
        public fun parseHex(hexString: String): UUID =
            uuidFromHexString(hexString)

        // TODO: Decide on naming scheme
        // v4
        public fun randomUUIDv4(): UUID {
            return secureRandomUUID()
        }
//
//        // v3
//        public fun nameBasedUUIDv3(namespace: UUID, nameBytes: ByteArray): UUID {
//
//        }
//
//        // v5
//        public fun nameBasedUUIDv5(namespace: UUID, nameBytes: ByteArray): UUID {
//
//        }
//
//        // v7
//        public fun timeOrderedUUIDv7(): UUID {
//
//        }

        // TODO: Name
        //   * BIG_ENDIAN_BITWISE_ORDER
        //   * BITS_ORDER
        //   * BITWISE_COMPARATOR
        public val BITWISE_ORDER: Comparator<UUID>
            get() = UUID_BITWISE_ORDER
    }
}
