@file:Suppress("NO_ACTUAL_FOR_EXPECT")

package kotlin.ranges

expect infix fun PlatformInt.downTo(to: PlatformInt): PlatformIntProgression
expect infix fun PlatformUInt.downTo(to: PlatformUInt): PlatformUIntProgression
expect infix fun PlatformInt.until(to: PlatformInt): PlatformIntRange
expect infix fun PlatformUInt.until(to: PlatformUInt): PlatformUIntRange
