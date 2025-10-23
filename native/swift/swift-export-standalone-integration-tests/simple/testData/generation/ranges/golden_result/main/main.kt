@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("generation_ranges_ranges_accept__TypesOfArguments__Swift_ClosedRange_Swift_Int32___")
public fun generation_ranges_ranges_accept__TypesOfArguments__Swift_ClosedRange_Swift_Int32___(range_1: Int, range_2: Int): kotlin.native.internal.NativePtr {
    val __range = range_1..range_2
    val _result = generation.ranges.ranges.accept(__range)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_ranges_ranges_acceptClosed__TypesOfArguments__Swift_ClosedRange_Swift_Int32___")
public fun generation_ranges_ranges_acceptClosed__TypesOfArguments__Swift_ClosedRange_Swift_Int32___(range_1: Int, range_2: Int): kotlin.native.internal.NativePtr {
    val __range = range_1..range_2
    val _result = generation.ranges.ranges.acceptClosed(__range)
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

@ExportedBridge("kotlin_ranges_closedRange_getEndInclusive_int")
fun kotlin_ranges_closedRange_getEndInclusive_int(nativePtr: kotlin.native.internal.NativePtr): Int {
    val closedRange = interpretObjCPointer<ClosedRange<Int>>(nativePtr)
    return closedRange.endInclusive
}

@ExportedBridge("kotlin_ranges_closedRange_getStart_int")
fun kotlin_ranges_closedRange_getStart_int(nativePtr: kotlin.native.internal.NativePtr): Int {
    val closedRange = interpretObjCPointer<ClosedRange<Int>>(nativePtr)
    return closedRange.start
}

@ExportedBridge("kotlin_ranges_intRange_getEndInclusive_int")
fun kotlin_ranges_intRange_getEndInclusive_int(nativePtr: kotlin.native.internal.NativePtr): Int {
    val intRange = interpretObjCPointer<IntRange>(nativePtr)
    return intRange.endInclusive
}

@ExportedBridge("kotlin_ranges_intRange_getStart_int")
fun kotlin_ranges_intRange_getStart_int(nativePtr: kotlin.native.internal.NativePtr): Int {
    val intRange = interpretObjCPointer<IntRange>(nativePtr)
    return intRange.start
}

@ExportedBridge("kotlin_ranges_longRange_getEndInclusive_long")
fun kotlin_ranges_longRange_getEndInclusive_long(nativePtr: kotlin.native.internal.NativePtr): Long {
    val longRange = interpretObjCPointer<LongRange>(nativePtr)
    return longRange.endInclusive
}

@ExportedBridge("kotlin_ranges_longRange_getStart_long")
fun kotlin_ranges_longRange_getStart_long(nativePtr: kotlin.native.internal.NativePtr): Long {
    val longRange = interpretObjCPointer<LongRange>(nativePtr)
    return longRange.start
}

@ExportedBridge("kotlin_ranges_openEndRange_getEndExclusive_int")
fun kotlin_ranges_openEndRange_getEndExclusive_int(nativePtr: kotlin.native.internal.NativePtr): Int {
    val openEndRange = interpretObjCPointer<OpenEndRange<Int>>(nativePtr)
    return openEndRange.endExclusive
}

@ExportedBridge("kotlin_ranges_openEndRange_getEndExclusive_long")
fun kotlin_ranges_openEndRange_getEndExclusive_long(nativePtr: kotlin.native.internal.NativePtr): Long {
    val openEndRange = interpretObjCPointer<OpenEndRange<Long>>(nativePtr)
    return openEndRange.endExclusive
}

@ExportedBridge("kotlin_ranges_openEndRange_getStart_int")
fun kotlin_ranges_openEndRange_getStart_int(nativePtr: kotlin.native.internal.NativePtr): Int {
    val openEndRange = interpretObjCPointer<OpenEndRange<Int>>(nativePtr)
    return openEndRange.start
}

@ExportedBridge("kotlin_ranges_openEndRange_getStart_long")
fun kotlin_ranges_openEndRange_getStart_long(nativePtr: kotlin.native.internal.NativePtr): Long {
    val openEndRange = interpretObjCPointer<OpenEndRange<Long>>(nativePtr)
    return openEndRange.start
}
