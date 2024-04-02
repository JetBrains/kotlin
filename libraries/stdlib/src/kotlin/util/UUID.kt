/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.random.Random

public open class UUID {
    internal val msb: Long
    internal val lsb: Long

    public constructor(mostSignificantBits: Long, leastSignificantBits: Long) {
        msb = mostSignificantBits
        lsb = leastSignificantBits
    }

    public constructor(bytes: ByteArray) {
        uuidFromBytes(bytes) { msb, lsb ->
            this.msb = msb
            this.lsb = lsb
        }
    }

    public constructor(uuidString: String) {
        uuidFromString(uuidString) { msb, lsb ->
            this.msb = msb
            this.lsb = lsb
        }
    }

    public val mostSignificantBits: Long
        get() = msb

    public val leastSignificantBits: Long
        get() = lsb

    public val isIETFVariant: Boolean
        get() = (lsb ushr 62).toInt() == 2

    public val version: Int
        get() = uuidVersion(this)

    /** 8-4-4-4-12 hex format in lower case. */
    override fun toString(): String {
        return toString(upperCase = false)
    }

    /** 8-4-4-4-12 hex format in the specified case. */
    public fun toString(upperCase: Boolean): String {
        return uuidToString(this, upperCase)
    }

    // TODO: Introduce HexFormat.uuid scope for defining the uuid format?
    //   * with/without hyphens
    //   * with/without braces
    //   * with prefix, e.g., "urn:uuid:" or "0x"
    //   * with suffix, e.g., "h"
    /** Hex format without hyphens in the specified case. */
    public fun toHexString(upperCase: Boolean = true): String {
        return uuidToHexString(this, upperCase)
    }

    public fun toByteArray(): ByteArray {
        return uuidToByteArray(this)
    }

    public companion object {
        public val MIN_VALUE: UUID = UUID(0, 0)
        public val MAX_VALUE: UUID = UUID(-1, -1)

        // v4
        public fun randomUUIDv4(): UUID {
            return secureRandomUUID()
        }

        public fun randomUUIDGenerator(random: Random): UUIDGenerator {

        }

        // v3
        public fun nameBasedUUIDv3(namespace: UUID, nameBytes: ByteArray): UUID {

        }

        // v5
        public fun nameBasedUUIDv5(namespace: UUID, nameBytes: ByteArray): UUID {

        }

        // v7
        public fun timeOrderedUUIDv7(): UUID {

        }
    }

    public interface UUIDGenerator {
        public fun nextUUID(): UUID
    }
}

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
public val UUID.Companion.TIMESTAMP_ORDER: Comparator<UUID>
    get() = UUID_TIMESTAMP_ORDER

// TODO: Name
//   * BIG_ENDIAN_BITWISE_ORDER
//   * BITS_ORDER
//   * BITWISE_COMPARATOR
public val UUID.Companion.BITWISE_ORDER: Comparator<UUID>
    get() = UUID_BITWISE_ORDER

