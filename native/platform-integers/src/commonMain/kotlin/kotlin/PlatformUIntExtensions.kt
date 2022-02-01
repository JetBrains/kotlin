@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NO_ACTUAL_FOR_EXPECT", "PHANTOM_CLASSIFIER", "LEAKING_PHANTOM_TYPE")

package kotlin

expect fun PlatformUInt.countLeadingZeroBits(): Int
expect fun PlatformUInt.countOneBits(): Int
expect fun PlatformUInt.countTrailingZeroBits(): Int

expect fun PlatformUInt.rotateLeft(bitCount: Int): PlatformUInt
expect fun PlatformUInt.rotateRight(bitCount: Int): PlatformUInt

expect fun PlatformUInt.takeHighestOneBit(): PlatformUInt
expect fun PlatformUInt.takeLowestOneBit(): PlatformUInt
