@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("kotlin_ranges_ClosedRange_contains__TypesOfArguments__T____reverse_swift")
internal external fun kotlin_ranges_ClosedRange_contains__TypesOfArguments__T____reverse_swift(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.ranges.ClosedRange::class, "contains")
public fun kotlin_ranges_ClosedRange_contains__TypesOfArguments__T____reverse(self: kotlin.ranges.ClosedRange<*>, value: kotlin.Comparable<*>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __value = if (value == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(value)
    val _result = kotlin_ranges_ClosedRange_contains__TypesOfArguments__T____reverse_swift(__self, __value)
    return _result
}

@ImportedBridge("kotlin_ranges_ClosedRange_endInclusive_get__reverse_swift")
internal external fun kotlin_ranges_ClosedRange_endInclusive_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.ranges.ClosedRange::class, "<get-endInclusive>")
public fun kotlin_ranges_ClosedRange_endInclusive_get__reverse(self: kotlin.ranges.ClosedRange<*>): kotlin.Comparable<*> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_ranges_ClosedRange_endInclusive_get__reverse_swift(__self)
    return (dereferenceAndDisposeBridgeableRef(_result) as kotlin.Comparable<*>)
}

@ImportedBridge("kotlin_ranges_ClosedRange_isEmpty__reverse_swift")
internal external fun kotlin_ranges_ClosedRange_isEmpty__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.ranges.ClosedRange::class, "isEmpty")
public fun kotlin_ranges_ClosedRange_isEmpty__reverse(self: kotlin.ranges.ClosedRange<*>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_ranges_ClosedRange_isEmpty__reverse_swift(__self)
    return _result
}

@ImportedBridge("kotlin_ranges_ClosedRange_start_get__reverse_swift")
internal external fun kotlin_ranges_ClosedRange_start_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.ranges.ClosedRange::class, "<get-start>")
public fun kotlin_ranges_ClosedRange_start_get__reverse(self: kotlin.ranges.ClosedRange<*>): kotlin.Comparable<*> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_ranges_ClosedRange_start_get__reverse_swift(__self)
    return (dereferenceAndDisposeBridgeableRef(_result) as kotlin.Comparable<*>)
}

@ExportedBridge("kotlin_ranges_ClosedRange_contains__TypesOfArguments__T__")
public fun kotlin_ranges_ClosedRange_contains__TypesOfArguments__T__(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.ClosedRange<*>
    val __value = (dereferenceAndDisposeBridgeableRef(value) as kotlin.Comparable<*>)
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : kotlin.Comparable<__T0>> __bridge(): Boolean = ((__self as kotlin.ranges.ClosedRange<__T0>).contains((__value as __T0)) as kotlin.Any?) as Boolean
        __bridge<Nothing>()
    }
                    return _result
}

@ExportedBridge("kotlin_ranges_ClosedRange_contains__TypesOfArguments__T___direct", nonVirtualTargetMethod = "contains")
public fun kotlin_ranges_ClosedRange_contains__TypesOfArguments__T___direct(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.ClosedRange<*>
    val __value = (dereferenceAndDisposeBridgeableRef(value) as kotlin.Comparable<*>)
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : kotlin.Comparable<__T0>> __bridge(): Boolean = ((__self as kotlin.ranges.ClosedRange<__T0>).contains((__value as __T0)) as kotlin.Any?) as Boolean
        __bridge<Nothing>()
    }
                    return _result
}

@ExportedBridge("kotlin_ranges_ClosedRange_endInclusive_get")
public fun kotlin_ranges_ClosedRange_endInclusive_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.ClosedRange<*>
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : kotlin.Comparable<__T0>> __bridge(): kotlin.Comparable<*> = ((__self as kotlin.ranges.ClosedRange<__T0>).endInclusive as kotlin.Any?) as kotlin.Comparable<*>
        __bridge<Nothing>()
    }
                    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_ranges_ClosedRange_isEmpty")
public fun kotlin_ranges_ClosedRange_isEmpty(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.ClosedRange<*>
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : kotlin.Comparable<__T0>> __bridge(): Boolean = ((__self as kotlin.ranges.ClosedRange<__T0>).isEmpty() as kotlin.Any?) as Boolean
        __bridge<Nothing>()
    }
                    return _result
}

@ExportedBridge("kotlin_ranges_ClosedRange_isEmpty_direct", nonVirtualTargetMethod = "isEmpty")
public fun kotlin_ranges_ClosedRange_isEmpty_direct(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.ClosedRange<*>
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : kotlin.Comparable<__T0>> __bridge(): Boolean = ((__self as kotlin.ranges.ClosedRange<__T0>).isEmpty() as kotlin.Any?) as Boolean
        __bridge<Nothing>()
    }
                    return _result
}

@ExportedBridge("kotlin_ranges_ClosedRange_start_get")
public fun kotlin_ranges_ClosedRange_start_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.ClosedRange<*>
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : kotlin.Comparable<__T0>> __bridge(): kotlin.Comparable<*> = ((__self as kotlin.ranges.ClosedRange<__T0>).start as kotlin.Any?) as kotlin.Comparable<*>
        __bridge<Nothing>()
    }
                    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
