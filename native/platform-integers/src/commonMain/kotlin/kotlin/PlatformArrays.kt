@file:Suppress("NO_ACTUAL_FOR_EXPECT", "PHANTOM_CLASSIFIER", "LEAKING_PHANTOM_TYPE", "LEAKING_PHANTOM_TYPE_IN_SUPERTYPES")

package kotlin

expect class PlatformIntArray(size: Int) {
    public constructor(size: Int, init: (Int) -> PlatformInt)

    public operator fun get(index: Int): PlatformInt
    public operator fun set(index: Int, value: PlatformInt): Unit
    public val size: Int
    public operator fun iterator(): PlatformIntIterator
}

expect abstract class PlatformIntIterator : Iterator<PlatformInt>

expect class PlatformUIntArray(size: Int) {
    public constructor(size: Int, init: (Int) -> PlatformUInt)

    public operator fun get(index: Int): PlatformUInt
    public operator fun set(index: Int, value: PlatformUInt): Unit
    public val size: Int

    @Suppress("DEPRECATION_ERROR")
    public operator fun iterator(): PlatformUIntIterator
}

@Deprecated("This class is not going to be stabilized and is to be removed soon.", level = DeprecationLevel.ERROR)
expect abstract class PlatformUIntIterator : Iterator<PlatformUInt>
