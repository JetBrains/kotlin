@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Comparable::class, "_Comparable")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("kotlin_Comparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_Comparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Comparable<kotlin.Any?>
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = __self.compareTo(__other)
    return _result
}

@ExportedBridge("kotlin_ranges_ClosedRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable__")
public fun kotlin_ranges_ClosedRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable__(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.ClosedRange<kotlin.Comparable<kotlin.Any?>>
    val __value = kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Comparable<kotlin.Any?>
    val _result = __self.contains(__value)
    return _result
}

@ExportedBridge("kotlin_ranges_ClosedRange_endInclusive_get")
public fun kotlin_ranges_ClosedRange_endInclusive_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.ClosedRange<kotlin.Comparable<kotlin.Any?>>
    val _result = __self.endInclusive
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_ranges_ClosedRange_isEmpty")
public fun kotlin_ranges_ClosedRange_isEmpty(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.ClosedRange<kotlin.Comparable<kotlin.Any?>>
    val _result = __self.isEmpty()
    return _result
}

@ExportedBridge("kotlin_ranges_ClosedRange_start_get")
public fun kotlin_ranges_ClosedRange_start_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.ClosedRange<kotlin.Comparable<kotlin.Any?>>
    val _result = __self.start
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
