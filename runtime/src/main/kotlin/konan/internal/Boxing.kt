/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package konan.internal

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

@ExportForCppRuntime("Kotlin_boxFloat")
fun boxFloat(value: Float): Float? = value

@ExportForCppRuntime("Kotlin_boxDouble")
fun boxDouble(value: Double): Double? = value
