@file:Suppress(
    "DEPRECATED_BINARY_MOD",
    "NO_ACTUAL_FOR_EXPECT",
    "PHANTOM_CLASSIFIER",
    "LEAKING_PHANTOM_TYPE",
    "LEAKING_PHANTOM_TYPE_IN_SUPERTYPES"
)

package kotlin

expect value class PlatformUInt internal constructor(internal val data: PlatformInt) : Comparable<PlatformUInt> {

    companion object {
        val MAX_VALUE: PlatformUInt
        val MIN_VALUE: PlatformUInt
        val SIZE_BITS: Int
        val SIZE_BYTES: Int
    }

    inline fun inv(): PlatformUInt
    inline infix fun and(other: PlatformUInt): PlatformUInt
    inline infix fun or(other: PlatformUInt): PlatformUInt
    inline infix fun xor(other: PlatformUInt): PlatformUInt
    inline infix fun shl(bitCount: Int): PlatformUInt
    inline infix fun shr(bitCount: Int): PlatformUInt

    override operator fun compareTo(other: PlatformUInt): Int

    operator fun div(other: PlatformUInt): PlatformUInt
    operator fun minus(other: PlatformUInt): PlatformUInt
    operator fun plus(other: PlatformUInt): PlatformUInt
    operator fun rem(other: PlatformUInt): PlatformUInt
    operator fun times(other: PlatformUInt): PlatformUInt

    inline fun floorDiv(other: PlatformUInt): PlatformUInt
    operator fun mod(other: UByte): UByte
    operator fun mod(other: UShort): UShort
    operator fun mod(other: UInt): UInt
    operator fun mod(other: ULong): ULong

    inline fun toByte(): Byte
    inline fun toDouble(): Double
    inline fun toFloat(): Float
    inline fun toInt(): Int
    inline fun toLong(): Long
    inline fun toShort(): Short
    inline fun toUByte(): UByte
    inline fun toUInt(): UInt
    inline fun toULong(): ULong
    inline fun toUShort(): UShort
}
