@file:Suppress("NO_ACTUAL_FOR_EXPECT", "PHANTOM_CLASSIFIER", "LEAKING_PHANTOM_TYPE", "LEAKING_PHANTOM_TYPE_IN_SUPERTYPES")

package kotlin.ranges

expect open class PlatformUIntProgression
internal constructor
    (
    start: PlatformUInt,
    endInclusive: PlatformUInt,
    step: PlatformUInt
) : Iterable<PlatformUInt> {
    val first: PlatformUInt
    val last: PlatformUInt
    val step: PlatformInt

    open fun isEmpty(): Boolean

    companion object {
        fun fromClosedRange(
            rangeStart: PlatformUInt,
            rangeEnd: PlatformUInt,
            step: PlatformInt
        ): PlatformUIntProgression
    }
}
