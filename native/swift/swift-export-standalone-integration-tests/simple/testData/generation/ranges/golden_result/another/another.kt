@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("kotlin_ranges_intRange_getEndInclusive_int_another")
fun kotlin_ranges_intRange_getEndInclusive_int_another(nativePtr: kotlin.native.internal.NativePtr): Int {
    val intRange = kotlin.native.internal.ref.dereferenceExternalRCRef(nativePtr) as IntRange
    return intRange.endInclusive
}

@ExportedBridge("kotlin_ranges_intRange_getStart_int_another")
fun kotlin_ranges_intRange_getStart_int_another(nativePtr: kotlin.native.internal.NativePtr): Int {
    val intRange = kotlin.native.internal.ref.dereferenceExternalRCRef(nativePtr) as IntRange
    return intRange.start
}

@ExportedBridge("some_foo")
public fun some_foo(): kotlin.native.internal.NativePtr {
    val _result = some.foo()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
