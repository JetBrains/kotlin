@file:Suppress("NO_ACTUAL_FOR_EXPECT")

package kotlin.ranges

expect class PlatformUIntRange : PlatformUIntProgression, ClosedRange<PlatformUInt> {
    override fun isEmpty(): Boolean

    companion object {
        val EMPTY: PlatformIntRange
    }
}
