@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER", "NO_ACTUAL_FOR_EXPECT", "PHANTOM_CLASSIFIER", "LEAKING_PHANTOM_TYPE")

package kotlin


expect fun PlatformInt.toUByte(): UByte
expect fun PlatformInt.toUShort(): UShort
expect fun PlatformInt.toUInt(): UInt
expect fun PlatformInt.toULong(): ULong

// Class members for UInt
expect inline fun PlatformInt.floorDiv(other: PlatformInt): PlatformInt

// Class members for UInt
expect fun PlatformInt.mod(other: Byte): Byte
expect fun PlatformInt.mod(other: Short): Short
expect fun PlatformInt.mod(other: Int): Int
expect fun PlatformInt.mod(other: Long): Long

expect fun PlatformInt.countLeadingZeroBits(): Int
expect fun PlatformInt.countOneBits(): Int
expect fun PlatformInt.countTrailingZeroBits(): Int

@ExperimentalStdlibApi
expect fun PlatformInt.rotateLeft(bitCount: Int): PlatformInt
@ExperimentalStdlibApi
expect fun PlatformInt.rotateRight(bitCount: Int): PlatformInt

expect fun PlatformInt.takeHighestOneBit(): PlatformInt
expect fun PlatformInt.takeLowestOneBit(): PlatformInt
