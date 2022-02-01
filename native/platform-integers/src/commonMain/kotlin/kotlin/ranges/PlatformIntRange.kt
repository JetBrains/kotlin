@file:Suppress("NO_ACTUAL_FOR_EXPECT", "PHANTOM_CLASSIFIER", "LEAKING_PHANTOM_TYPE", "LEAKING_PHANTOM_TYPE_IN_SUPERTYPES")

package kotlin.ranges

expect class PlatformIntRange : PlatformIntProgression, ClosedRange<PlatformInt> {
    override fun isEmpty(): Boolean

    companion object {
        val EMPTY: PlatformIntRange
    }
}
