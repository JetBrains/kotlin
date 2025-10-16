@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Comparable::class, "_Comparable")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.IntIterator::class, "22ExportedKotlinPackages6kotlinO11collectionsO12KotlinStdlibE11IntIteratorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.ranges.IntProgression::class, "22ExportedKotlinPackages6kotlinO6rangesO12KotlinStdlibE14IntProgressionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.ranges.IntProgression.Companion::class, "22ExportedKotlinPackages6kotlinO6rangesO12KotlinStdlibE14IntProgressionC9CompanionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.ranges.IntRange::class, "22ExportedKotlinPackages6kotlinO6rangesO12KotlinStdlibE8IntRangeC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.ranges.IntRange.Companion::class, "22ExportedKotlinPackages6kotlinO6rangesO12KotlinStdlibE8IntRangeC9CompanionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.ranges.ClosedRange::class, "_ClosedRange")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.ranges.OpenEndRange::class, "_OpenEndRange")

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

@ExportedBridge("kotlin_collections_IntIterator_next")
public fun kotlin_collections_IntIterator_next(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.IntIterator
    val _result = __self.next()
    return _result
}

@ExportedBridge("kotlin_collections_IntIterator_nextInt")
public fun kotlin_collections_IntIterator_nextInt(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.IntIterator
    val _result = __self.nextInt()
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

@ExportedBridge("kotlin_ranges_IntProgression_Companion_fromClosedRange__TypesOfArguments__Swift_Int32_Swift_Int32_Swift_Int32__")
public fun kotlin_ranges_IntProgression_Companion_fromClosedRange__TypesOfArguments__Swift_Int32_Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, rangeStart: Int, rangeEnd: Int, step: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntProgression.Companion
    val __rangeStart = rangeStart
    val __rangeEnd = rangeEnd
    val __step = step
    val _result = __self.fromClosedRange(__rangeStart, __rangeEnd, __step)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_ranges_IntProgression_Companion_get")
public fun kotlin_ranges_IntProgression_Companion_get(): kotlin.native.internal.NativePtr {
    val _result = kotlin.ranges.IntProgression.Companion
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_ranges_IntProgression_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_ranges_IntProgression_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntProgression
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = __self.equals(__other)
    return _result
}

@ExportedBridge("kotlin_ranges_IntProgression_first_get")
public fun kotlin_ranges_IntProgression_first_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntProgression
    val _result = __self.first
    return _result
}

@ExportedBridge("kotlin_ranges_IntProgression_hashCode")
public fun kotlin_ranges_IntProgression_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntProgression
    val _result = __self.hashCode()
    return _result
}

@ExportedBridge("kotlin_ranges_IntProgression_isEmpty")
public fun kotlin_ranges_IntProgression_isEmpty(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntProgression
    val _result = __self.isEmpty()
    return _result
}

@ExportedBridge("kotlin_ranges_IntProgression_iterator")
public fun kotlin_ranges_IntProgression_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntProgression
    val _result = __self.iterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_ranges_IntProgression_last_get")
public fun kotlin_ranges_IntProgression_last_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntProgression
    val _result = __self.last
    return _result
}

@ExportedBridge("kotlin_ranges_IntProgression_step_get")
public fun kotlin_ranges_IntProgression_step_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntProgression
    val _result = __self.step
    return _result
}

@ExportedBridge("kotlin_ranges_IntProgression_toString")
public fun kotlin_ranges_IntProgression_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntProgression
    val _result = __self.toString()
    return _result.objcPtr()
}

@ExportedBridge("kotlin_ranges_IntRange_Companion_EMPTY_get")
public fun kotlin_ranges_IntRange_Companion_EMPTY_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntRange.Companion
    val _result = __self.EMPTY
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_ranges_IntRange_Companion_get")
public fun kotlin_ranges_IntRange_Companion_get(): kotlin.native.internal.NativePtr {
    val _result = kotlin.ranges.IntRange.Companion
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_ranges_IntRange_contains__TypesOfArguments__Swift_Int32__")
public fun kotlin_ranges_IntRange_contains__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, value: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntRange
    val __value = value
    val _result = __self.contains(__value)
    return _result
}

@ExportedBridge("kotlin_ranges_IntRange_endExclusive_get")
public fun kotlin_ranges_IntRange_endExclusive_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntRange
    val _result = __self.endExclusive
    return _result
}

@ExportedBridge("kotlin_ranges_IntRange_endInclusive_get")
public fun kotlin_ranges_IntRange_endInclusive_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntRange
    val _result = __self.endInclusive
    return _result
}

@ExportedBridge("kotlin_ranges_IntRange_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_ranges_IntRange_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntRange
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = __self.equals(__other)
    return _result
}

@ExportedBridge("kotlin_ranges_IntRange_hashCode")
public fun kotlin_ranges_IntRange_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntRange
    val _result = __self.hashCode()
    return _result
}

@ExportedBridge("kotlin_ranges_IntRange_init_allocate")
public fun kotlin_ranges_IntRange_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<kotlin.ranges.IntRange>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_ranges_IntRange_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__")
public fun kotlin_ranges_IntRange_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, start: Int, endInclusive: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __start = start
    val __endInclusive = endInclusive
    kotlin.native.internal.initInstance(____kt, kotlin.ranges.IntRange(__start, __endInclusive))
}

@ExportedBridge("kotlin_ranges_IntRange_isEmpty")
public fun kotlin_ranges_IntRange_isEmpty(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntRange
    val _result = __self.isEmpty()
    return _result
}

@ExportedBridge("kotlin_ranges_IntRange_start_get")
public fun kotlin_ranges_IntRange_start_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntRange
    val _result = __self.start
    return _result
}

@ExportedBridge("kotlin_ranges_IntRange_toString")
public fun kotlin_ranges_IntRange_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.IntRange
    val _result = __self.toString()
    return _result.objcPtr()
}

@ExportedBridge("kotlin_ranges_OpenEndRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable__")
public fun kotlin_ranges_OpenEndRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable__(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.OpenEndRange<kotlin.Comparable<kotlin.Any?>>
    val __value = kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Comparable<kotlin.Any?>
    val _result = __self.contains(__value)
    return _result
}

@ExportedBridge("kotlin_ranges_OpenEndRange_endExclusive_get")
public fun kotlin_ranges_OpenEndRange_endExclusive_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.OpenEndRange<kotlin.Comparable<kotlin.Any?>>
    val _result = __self.endExclusive
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_ranges_OpenEndRange_isEmpty")
public fun kotlin_ranges_OpenEndRange_isEmpty(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.OpenEndRange<kotlin.Comparable<kotlin.Any?>>
    val _result = __self.isEmpty()
    return _result
}

@ExportedBridge("kotlin_ranges_OpenEndRange_start_get")
public fun kotlin_ranges_OpenEndRange_start_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ranges.OpenEndRange<kotlin.Comparable<kotlin.Any?>>
    val _result = __self.start
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
