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
public class UUID internal constructor(internal val msb: Long, internal val lsb: Long) {

    /** Returns the most significant 64 bits of this UUID. */
    public val mostSignificantBits: Long
        get() = msb

    /** Returns the least significant 64 bits of this UUID. */
    public val leastSignificantBits: Long
        get() = lsb

    /**
     * Executes a specified block of code, providing access to the UUID's bits.
     * This function is intended for use when one needs to perform bitwise operations with the UUID.
     *
     * The [block] will receive two [Long] arguments:
     *   - `mostSignificantBits`: The most significant 64 bits of this UUID.
     *   - `leastSignificantBits`: The least significant 64 bits of this UUID.
     *
     * @param block A function that takes two [Long] arguments (mostSignificantBits, leastSignificantBits).
     *   This block is guaranteed to be called exactly once.
     */
    // Perhaps should be removed in favour of most/leastSignificantBits properties
    @InlineOnly
    public inline fun toLongs(block: (mostSignificantBits: Long, leastSignificantBits: Long) -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        block(mostSignificantBits, leastSignificantBits)
    }

    /** Indicates whether this UUID is of the IETF variant (variant 2). */
    public val isIETFVariant: Boolean
        get() = (lsb ushr 62).toInt() == 2

    /**
     * Returns the version number of this UUID.
     *
     * The version number describes the algorithm used for generating the UUID.
     */
    public val version: Int
        get() = uuidVersion(this)

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
        // TODO: UUIDs are not comparable. Thus, makes sense to rename to ALL_ZERO, or similar.
        //   Or use the standard name NIL.
        /** The minimum possible UUID (all bits set to zero). */
        public val MIN_VALUE: UUID = UUID(0, 0)

        // TODO: UUIDs are not comparable. Thus, makes sense to rename to ALL_ONE, or similar.
        //   Or use the standard name MAX.
        /** The maximum possible UUID (all bits set to one). */
        public val MAX_VALUE: UUID = UUID(-1, -1)

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

        // TODO: Should we allow comparing timestamps of different versions?
        //   * v1 and v6
        //   * v1 and v7 (they have different starting points and granularity)
        // TODO: Name
        //   * public object UUIDTimestampComparator
        //   * public val UUID.Companion.TimestampOrder
        //   * public val UUID.Companion.TIMESTAMP_COMPARATOR
        //   * public val UUID.Companion.TimestampComparator
        // TODO: What if in the future a new UUID version with timestamp gets introduced?
        //   * Perhaps we should document which versions can be compared with timestamp
        // TODO: If the timestamp value is equal
        //   * should the clock sequence be compared in v1 and v6?
        //     * Probably not, clock sequence could be [re]generated randomly
        //   * in v7 the subsequent bits could be used for sub-millisecond granularity
        //     * Bitwise comparison could be used to account for those bits
        //   * this comparator should only check the timestamp bits as specified by standard, as the comparator name indicates
        public val TIMESTAMP_ORDER: Comparator<UUID>
            get() = UUID_TIMESTAMP_ORDER

        // TODO: Name
        //   * BIG_ENDIAN_BITWISE_ORDER
        //   * BITS_ORDER
        //   * BITWISE_COMPARATOR
        public val BITWISE_ORDER: Comparator<UUID>
            get() = UUID_BITWISE_ORDER
    }
}
