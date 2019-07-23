/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

@SymbolName("getCachedBooleanBox")
external fun getCachedBooleanBox(value: Boolean): Boolean?
@SymbolName("inBooleanBoxCache")
external fun inBooleanBoxCache(value: Boolean): Boolean
@SymbolName("getCachedByteBox")
external fun getCachedByteBox(value: Byte): Byte?
@SymbolName("inByteBoxCache")
external fun inByteBoxCache(value: Byte): Boolean
@SymbolName("getCachedCharBox")
external fun getCachedCharBox(value: Char): Char?
@SymbolName("inCharBoxCache")
external fun inCharBoxCache(value: Char): Boolean
@SymbolName("getCachedShortBox")
external fun getCachedShortBox(value: Short): Short?
@SymbolName("inShortBoxCache")
external fun inShortBoxCache(value: Short): Boolean
@SymbolName("getCachedIntBox")
external fun getCachedIntBox(idx: Int): Int?
@SymbolName("inIntBoxCache")
external fun inIntBoxCache(value: Int): Boolean
@SymbolName("getCachedLongBox")
external fun getCachedLongBox(value: Long): Long?
@SymbolName("inLongBoxCache")
external fun inLongBoxCache(value: Long): Boolean

// TODO: functions below are used for ObjCExport, move and rename them correspondigly.

@ExportForCppRuntime("Kotlin_boxBoolean")
fun boxBoolean(value: Boolean): Boolean? = value

@ExportForCppRuntime("Kotlin_boxChar")
fun boxChar(value: Char): Char? = value

@ExportForCppRuntime("Kotlin_boxByte")
fun boxByte(value: Byte): Byte? = value

@ExportForCppRuntime("Kotlin_boxShort")
fun boxShort(value: Short): Short? = value

@ExportForCppRuntime("Kotlin_boxInt")
fun boxInt(value: Int): Int? = value

@ExportForCppRuntime("Kotlin_boxLong")
fun boxLong(value: Long): Long? = value

@ExperimentalUnsignedTypes
@ExportForCppRuntime("Kotlin_boxUByte")
fun boxUByte(value: UByte): UByte? = value

@ExperimentalUnsignedTypes
@ExportForCppRuntime("Kotlin_boxUShort")
fun boxUShort(value: UShort): UShort? = value

@ExperimentalUnsignedTypes
@ExportForCppRuntime("Kotlin_boxUInt")
fun boxUInt(value: UInt): UInt? = value

@ExperimentalUnsignedTypes
@ExportForCppRuntime("Kotlin_boxULong")
fun boxULong(value: ULong): ULong? = value

@ExportForCppRuntime("Kotlin_boxFloat")
fun boxFloat(value: Float): Float? = value

@ExportForCppRuntime("Kotlin_boxDouble")
fun boxDouble(value: Double): Double? = value

@ExportForCppRuntime("Kotlin_boxUnit")
internal fun Kotlin_boxUnit(): Unit? = Unit