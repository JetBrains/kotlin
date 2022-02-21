@file:Suppress("NO_ACTUAL_FOR_EXPECT")

package kotlin.ranges

expect open class PlatformIntProgression
internal constructor
    (
    start: PlatformInt,
    endInclusive: PlatformInt,
    step: PlatformInt
) : Iterable<PlatformInt> {
    val first: PlatformInt
    val last: PlatformInt
    val step: PlatformInt

    open fun isEmpty(): Boolean

    companion object {
        fun fromClosedRange(rangeStart: PlatformInt, rangeEnd: PlatformInt, step: PlatformInt): PlatformIntProgression
    }
}
