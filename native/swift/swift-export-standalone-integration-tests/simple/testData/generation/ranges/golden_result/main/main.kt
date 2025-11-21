@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("generation_ranges_ranges_accept__TypesOfArguments__Swift_ClosedRange_Swift_Int32___")
public fun generation_ranges_ranges_accept__TypesOfArguments__Swift_ClosedRange_Swift_Int32___(range_1: Int, range_2: Int): kotlin.native.internal.NativePtr {
    val __range = range_1 .. range_2
    val _result = generation.ranges.ranges.accept(__range)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_ranges_ranges_acceptClosed__TypesOfArguments__Swift_ClosedRange_Swift_Int32___")
public fun generation_ranges_ranges_acceptClosed__TypesOfArguments__Swift_ClosedRange_Swift_Int32___(range_1: Int, range_2: Int): kotlin.native.internal.NativePtr {
    val __range = range_1 .. range_2
    val _result = generation.ranges.ranges.acceptClosed(__range)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_ranges_ranges_acceptNullable__TypesOfArguments__Swift_Optional_Swift_ClosedRange_Swift_Int32____")
public fun generation_ranges_ranges_acceptNullable__TypesOfArguments__Swift_Optional_Swift_ClosedRange_Swift_Int32____(range_1: kotlin.native.internal.NativePtr, range_2: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __range = if (range_1 == kotlin.native.internal.NativePtr.NULL || range_2 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Int>(range_1) .. interpretObjCPointer<Int>(range_2)
    val _result = generation.ranges.ranges.acceptNullable(__range)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
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

@ExportedBridge("generation_ranges_ranges_mapper__TypesOfArguments__Swift_Array_Swift_ClosedRange_Swift_Int64____")
public fun generation_ranges_ranges_mapper__TypesOfArguments__Swift_Array_Swift_ClosedRange_Swift_Int64____(arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __arg = interpretObjCPointer<kotlin.collections.List<kotlin.ranges.LongRange>>(arg)
    val _result = generation.ranges.ranges.mapper(__arg)
    return _result.objcPtr()
}

@ExportedBridge("generation_ranges_ranges_total__TypesOfArguments__Swift_Array_Swift_ClosedRange_Swift_Int32____")
public fun generation_ranges_ranges_total__TypesOfArguments__Swift_Array_Swift_ClosedRange_Swift_Int32____(list: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __list = interpretObjCPointer<kotlin.collections.List<kotlin.ranges.IntRange>>(list)
    val _result = generation.ranges.ranges.total(__list)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_ranges_ranges_unsupported")
public fun generation_ranges_ranges_unsupported(): kotlin.native.internal.NativePtr {
    val _result = generation.ranges.ranges.unsupported()
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
