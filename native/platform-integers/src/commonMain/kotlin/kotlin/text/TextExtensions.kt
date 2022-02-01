@file:Suppress("NO_ACTUAL_FOR_EXPECT", "PHANTOM_CLASSIFIER", "LEAKING_PHANTOM_TYPE")

package kotlin.text

expect inline fun PlatformInt.toString(radix: Int): String
expect inline fun PlatformUInt.toString(radix: Int): String
