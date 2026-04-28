@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Comparable::class, "_Comparable")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ImportedBridge("kotlin_Comparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_Comparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(kotlin.Comparable::class, "compareTo")
public fun kotlin_Comparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.Comparable<kotlin.Any?>, other: kotlin.Any?): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __other = if (other == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(other)
    val __result = kotlin_Comparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __other)
    return __result
}

@ImportedBridge("kotlin_ranges_ClosedRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable____reverse_swift")
internal external fun kotlin_ranges_ClosedRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable____reverse_swift(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.ranges.ClosedRange::class, "contains")
public fun kotlin_ranges_ClosedRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable____reverse(self: kotlin.ranges.ClosedRange<kotlin.Comparable<kotlin.Any?>>, value: kotlin.Comparable<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __value = kotlin.native.internal.ref.createRetainedExternalRCRef(value)
    val __result = kotlin_ranges_ClosedRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable____reverse_swift(__self, __value)
    return __result
}

@ImportedBridge("kotlin_ranges_ClosedRange_isEmpty__reverse_swift")
internal external fun kotlin_ranges_ClosedRange_isEmpty__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.ranges.ClosedRange::class, "isEmpty")
public fun kotlin_ranges_ClosedRange_isEmpty__reverse(self: kotlin.ranges.ClosedRange<kotlin.Comparable<kotlin.Any?>>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_ranges_ClosedRange_isEmpty__reverse_swift(__self)
    return __result
}

@ExportedBridge("kotlin_Comparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_Comparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Comparable<kotlin.Any?>
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = run { __self.compareTo(__other) }
    return _result
}

@ExportedBridge("kotlin_ranges_ClosedRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable__")
public fun kotlin_ranges_ClosedRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable__(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.ClosedRange<kotlin.Comparable<kotlin.Any?>>
    val __value = kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Comparable<kotlin.Any?>
    val _result = run { __self.contains(__value) }
    return _result
}

@ExportedBridge("kotlin_ranges_ClosedRange_endInclusive_get")
public fun kotlin_ranges_ClosedRange_endInclusive_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.ClosedRange<kotlin.Comparable<kotlin.Any?>>
    val _result = run { __self.endInclusive }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_ranges_ClosedRange_isEmpty")
public fun kotlin_ranges_ClosedRange_isEmpty(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.ClosedRange<kotlin.Comparable<kotlin.Any?>>
    val _result = run { __self.isEmpty() }
    return _result
}

@ExportedBridge("kotlin_ranges_ClosedRange_start_get")
public fun kotlin_ranges_ClosedRange_start_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.ClosedRange<kotlin.Comparable<kotlin.Any?>>
    val _result = run { __self.start }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
