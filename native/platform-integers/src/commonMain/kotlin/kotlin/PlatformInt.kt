@file:Suppress("NO_ACTUAL_FOR_EXPECT", "PHANTOM_CLASSIFIER", "LEAKING_PHANTOM_TYPE", "LEAKING_PHANTOM_TYPE_IN_SUPERTYPES")

package kotlin

expect class PlatformInt private constructor() : Number, Comparable<PlatformInt> {
    companion object {
        val MAX_VALUE: PlatformInt
        val MIN_VALUE: PlatformInt
        val SIZE_BITS: Int
        val SIZE_BYTES: Int
    }

    override operator fun compareTo(other: PlatformInt): Int

    external infix fun shl(bitCount: Int): PlatformInt
    external infix fun shr(bitCount: Int): PlatformInt
    external infix fun ushr(bitCount: Int): PlatformInt
    external infix fun xor(other: PlatformInt): PlatformInt

    external infix fun and(other: PlatformInt): PlatformInt
    external fun inv(): PlatformInt
    external infix fun or(other: PlatformInt): PlatformInt

    external operator fun dec(): PlatformInt
    external operator fun inc(): PlatformInt

    fun equals(other: PlatformInt): Boolean

    operator fun rangeTo(other: PlatformInt): PlatformIntRange

    external operator fun minus(other: PlatformInt): PlatformInt
    external operator fun plus(other: PlatformInt): PlatformInt
    external operator fun rem(other: PlatformInt): PlatformInt
    external operator fun times(other: PlatformInt): PlatformInt
    external operator fun div(other: PlatformInt): PlatformInt

    operator fun unaryMinus(): PlatformInt
    operator fun unaryPlus(): PlatformInt
}
