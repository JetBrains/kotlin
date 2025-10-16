@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("generation_ranges_ranges_accept__TypesOfArguments__ExportedKotlinPackages_kotlin_ranges_IntRange__")
public fun generation_ranges_ranges_accept__TypesOfArguments__ExportedKotlinPackages_kotlin_ranges_IntRange__(range: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __range = kotlin.native.internal.ref.dereferenceExternalRCRef(range) as kotlin.ranges.IntRange
    val _result = generation.ranges.ranges.accept(__range)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_ranges_ranges_bar")
public fun generation_ranges_ranges_bar(): kotlin.native.internal.NativePtr {
    val _result = generation.ranges.ranges.bar()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_ranges_ranges_baz")
public fun generation_ranges_ranges_baz(): kotlin.native.internal.NativePtr {
    val _result = generation.ranges.ranges.baz()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_ranges_ranges_foo")
public fun generation_ranges_ranges_foo(): kotlin.native.internal.NativePtr {
    val _result = generation.ranges.ranges.foo()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_ranges_ranges_unsupported")
public fun generation_ranges_ranges_unsupported(): kotlin.native.internal.NativePtr {
    val _result = generation.ranges.ranges.unsupported()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
