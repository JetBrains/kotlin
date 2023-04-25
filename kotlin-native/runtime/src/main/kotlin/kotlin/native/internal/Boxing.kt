/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

@GCUnsafeCall("getCachedBooleanBox")
@PublishedApi internal external fun getCachedBooleanBox(value: Boolean): Boolean?

@GCUnsafeCall("inBooleanBoxCache")
@PublishedApi internal external fun inBooleanBoxCache(value: Boolean): Boolean

@GCUnsafeCall("getCachedByteBox")
@PublishedApi internal external fun getCachedByteBox(value: Byte): Byte?

@GCUnsafeCall("inByteBoxCache")
@PublishedApi internal external fun inByteBoxCache(value: Byte): Boolean

@GCUnsafeCall("getCachedCharBox")
@PublishedApi internal external fun getCachedCharBox(value: Char): Char?

@GCUnsafeCall("inCharBoxCache")
@PublishedApi internal external fun inCharBoxCache(value: Char): Boolean

@GCUnsafeCall("getCachedShortBox")
@PublishedApi internal external fun getCachedShortBox(value: Short): Short?

@GCUnsafeCall("inShortBoxCache")
@PublishedApi internal external fun inShortBoxCache(value: Short): Boolean

@GCUnsafeCall("getCachedIntBox")
@PublishedApi internal external fun getCachedIntBox(idx: Int): Int?

@GCUnsafeCall("inIntBoxCache")
@PublishedApi internal external fun inIntBoxCache(value: Int): Boolean

@GCUnsafeCall("getCachedLongBox")
@PublishedApi internal external fun getCachedLongBox(value: Long): Long?

@GCUnsafeCall("inLongBoxCache")
@PublishedApi internal external fun inLongBoxCache(value: Long): Boolean

// TODO: functions below are used for ObjCExport and CAdapterGenerator, move and rename them correspondingly.

@ExportForCppRuntime("Kotlin_boxBoolean")
@PublishedApi internal fun boxBoolean(value: Boolean): Boolean? = value

@ExportForCppRuntime("Kotlin_boxChar")
@PublishedApi internal fun boxChar(value: Char): Char? = value

@ExportForCppRuntime("Kotlin_boxByte")
@PublishedApi internal fun boxByte(value: Byte): Byte? = value

@ExportForCppRuntime("Kotlin_boxShort")
@PublishedApi internal fun boxShort(value: Short): Short? = value

@ExportForCppRuntime("Kotlin_boxInt")
@PublishedApi internal fun boxInt(value: Int): Int? = value

@ExportForCppRuntime("Kotlin_boxLong")
@PublishedApi internal fun boxLong(value: Long): Long? = value

@ExperimentalUnsignedTypes
@ExportForCppRuntime("Kotlin_boxUByte")
@PublishedApi internal fun boxUByte(value: UByte): UByte? = value

@ExperimentalUnsignedTypes 
@ExportForCppRuntime("Kotlin_boxUShort")
@PublishedApi internal fun boxUShort(value: UShort): UShort? = value

@ExperimentalUnsignedTypes
@ExportForCppRuntime("Kotlin_boxUInt")
@PublishedApi internal fun boxUInt(value: UInt): UInt? = value

@ExperimentalUnsignedTypes
@ExportForCppRuntime("Kotlin_boxULong")
@PublishedApi internal fun boxULong(value: ULong): ULong? = value

@ExportForCppRuntime("Kotlin_boxFloat")
@PublishedApi internal fun boxFloat(value: Float): Float? = value

@ExportForCppRuntime("Kotlin_boxDouble")
@PublishedApi internal fun boxDouble(value: Double): Double? = value

@ExportForCppRuntime("Kotlin_boxUnit")
@PublishedApi internal fun Kotlin_boxUnit(): Unit? = Unit

// Unbox fuctions

@ExportForCppRuntime("Kotlin_unboxBoolean")
@PublishedApi internal fun unboxBoolean(value: Boolean?): Boolean = value!!

@ExportForCppRuntime("Kotlin_unboxChar")
@PublishedApi internal fun unboxChar(value: Char?): Char = value!!

@ExportForCppRuntime("Kotlin_unboxByte")
@PublishedApi internal fun unboxByte(value: Byte?): Byte = value!!

@ExportForCppRuntime("Kotlin_unboxShort")
@PublishedApi internal fun unboxShort(value: Short?): Short = value!!

@ExportForCppRuntime("Kotlin_unboxInt")
@PublishedApi internal fun unboxInt(value: Int?): Int = value!!

@ExportForCppRuntime("Kotlin_unboxLong")
@PublishedApi internal fun unboxLong(value: Long?): Long = value!!

@ExperimentalUnsignedTypes
@ExportForCppRuntime("Kotlin_unboxUByte")
@PublishedApi internal fun unboxUByte(value: UByte?): UByte = value!!

@ExperimentalUnsignedTypes
@ExportForCppRuntime("Kotlin_unboxUShort")
@PublishedApi internal fun unboxUShort(value: UShort?): UShort = value!!

@ExperimentalUnsignedTypes
@ExportForCppRuntime("Kotlin_unboxUInt")
@PublishedApi internal fun unboxUInt(value: UInt?): UInt = value!!

@ExperimentalUnsignedTypes
@ExportForCppRuntime("Kotlin_unboxULong")
@PublishedApi internal fun unboxULong(value: ULong?): ULong = value!!

@ExportForCppRuntime("Kotlin_unboxFloat")
@PublishedApi internal fun unboxFloat(value: Float?): Float = value!!

@ExportForCppRuntime("Kotlin_unboxDouble")
@PublishedApi internal fun unboxDouble(value: Double?): Double = value!!
