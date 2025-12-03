@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___accept__TypesOfArguments__Swift_ClosedRange_Swift_Int32___")
public fun __root___accept__TypesOfArguments__Swift_ClosedRange_Swift_Int32___(range_1: Int, range_2: Int): kotlin.native.internal.NativePtr {
    val __range = range_1 .. range_2
    val _result = accept(__range)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___acceptClosed__TypesOfArguments__Swift_ClosedRange_Swift_Int32___")
public fun __root___acceptClosed__TypesOfArguments__Swift_ClosedRange_Swift_Int32___(range_1: Int, range_2: Int): kotlin.native.internal.NativePtr {
    val __range = range_1 .. range_2
    val _result = acceptClosed(__range)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___bar")
public fun __root___bar(): kotlin.native.internal.NativePtr {
    val _result = bar()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___baz")
public fun __root___baz(): kotlin.native.internal.NativePtr {
    val _result = baz()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___foo")
public fun __root___foo(): kotlin.native.internal.NativePtr {
    val _result = foo()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___unsupported")
public fun __root___unsupported(): kotlin.native.internal.NativePtr {
    val _result = unsupported()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_ranges_closedRange_getEndInclusive_int")
fun kotlin_ranges_closedRange_getEndInclusive_int(nativePtr: kotlin.native.internal.NativePtr): Int {
    val closedRange = kotlin.native.internal.ref.dereferenceExternalRCRef(nativePtr) as ClosedRange<Int>
    return closedRange.endInclusive
}

@ExportedBridge("kotlin_ranges_closedRange_getStart_int")
fun kotlin_ranges_closedRange_getStart_int(nativePtr: kotlin.native.internal.NativePtr): Int {
    val closedRange = kotlin.native.internal.ref.dereferenceExternalRCRef(nativePtr) as ClosedRange<Int>
    return closedRange.start
}

@ExportedBridge("kotlin_ranges_intRange_getEndInclusive_int")
fun kotlin_ranges_intRange_getEndInclusive_int(nativePtr: kotlin.native.internal.NativePtr): Int {
    val intRange = kotlin.native.internal.ref.dereferenceExternalRCRef(nativePtr) as IntRange
    return intRange.endInclusive
}

@ExportedBridge("kotlin_ranges_intRange_getStart_int")
fun kotlin_ranges_intRange_getStart_int(nativePtr: kotlin.native.internal.NativePtr): Int {
    val intRange = kotlin.native.internal.ref.dereferenceExternalRCRef(nativePtr) as IntRange
    return intRange.start
}

@ExportedBridge("kotlin_ranges_longRange_getEndInclusive_long")
fun kotlin_ranges_longRange_getEndInclusive_long(nativePtr: kotlin.native.internal.NativePtr): Long {
    val longRange = kotlin.native.internal.ref.dereferenceExternalRCRef(nativePtr) as LongRange
    return longRange.endInclusive
}

@ExportedBridge("kotlin_ranges_longRange_getStart_long")
fun kotlin_ranges_longRange_getStart_long(nativePtr: kotlin.native.internal.NativePtr): Long {
    val longRange = kotlin.native.internal.ref.dereferenceExternalRCRef(nativePtr) as LongRange
    return longRange.start
}

@ExportedBridge("kotlin_ranges_openEndRange_getEndExclusive_int")
fun kotlin_ranges_openEndRange_getEndExclusive_int(nativePtr: kotlin.native.internal.NativePtr): Int {
    val openEndRange = kotlin.native.internal.ref.dereferenceExternalRCRef(nativePtr) as OpenEndRange<Int>
    return openEndRange.endExclusive
}

@ExportedBridge("kotlin_ranges_openEndRange_getEndExclusive_long")
fun kotlin_ranges_openEndRange_getEndExclusive_long(nativePtr: kotlin.native.internal.NativePtr): Long {
    val openEndRange = kotlin.native.internal.ref.dereferenceExternalRCRef(nativePtr) as OpenEndRange<Long>
    return openEndRange.endExclusive
}

@ExportedBridge("kotlin_ranges_openEndRange_getStart_int")
fun kotlin_ranges_openEndRange_getStart_int(nativePtr: kotlin.native.internal.NativePtr): Int {
    val openEndRange = kotlin.native.internal.ref.dereferenceExternalRCRef(nativePtr) as OpenEndRange<Int>
    return openEndRange.start
}

@ExportedBridge("kotlin_ranges_openEndRange_getStart_long")
fun kotlin_ranges_openEndRange_getStart_long(nativePtr: kotlin.native.internal.NativePtr): Long {
    val openEndRange = kotlin.native.internal.ref.dereferenceExternalRCRef(nativePtr) as OpenEndRange<Long>
    return openEndRange.start
}
