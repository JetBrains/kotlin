@file:Suppress("NO_ACTUAL_FOR_EXPECT", "PHANTOM_CLASSIFIER", "LEAKING_PHANTOM_TYPE", "LEAKING_PHANTOM_TYPE_IN_SUPERTYPES")

package kotlin.ranges

expect class PlatformUIntRange : PlatformUIntProgression, ClosedRange<PlatformUInt> {
    override fun isEmpty(): Boolean

    companion object {
        val EMPTY: PlatformIntRange
    }
}
