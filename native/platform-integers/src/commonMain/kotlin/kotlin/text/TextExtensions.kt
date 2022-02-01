@file:Suppress("NO_ACTUAL_FOR_EXPECT")

package kotlin.text

expect inline fun PlatformInt.toString(radix: Int): String
expect inline fun PlatformUInt.toString(radix: Int): String
