@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("generation_ranges_ranges_bar")
public fun generation_ranges_ranges_bar(): kotlin.native.internal.NativePtr {
    val _result = generation.ranges.ranges.bar()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_ranges_ranges_foo")
public fun generation_ranges_ranges_foo(): kotlin.native.internal.NativePtr {
    val _result = generation.ranges.ranges.foo()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
