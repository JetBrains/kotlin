/*
 * Copyright 2010-2018 JetBrains s.r.o.
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
package konan

import konan.SymbolName

@SymbolName("Kotlin_ByteArray_getCharAt")
external fun ByteArray.charAt(index: Int): Char

@SymbolName("Kotlin_ByteArray_getShortAt")
external fun ByteArray.shortAt(index: Int): Short

@SymbolName("Kotlin_ByteArray_getIntAt")
external fun ByteArray.intAt(index: Int): Int

@SymbolName("Kotlin_ByteArray_getLongAt")
external fun ByteArray.longAt(index: Int): Long

@SymbolName("Kotlin_ByteArray_getFloatAt")
external fun ByteArray.floatAt(index: Int): Float

@SymbolName("Kotlin_ByteArray_getDoubleAt")
external fun ByteArray.doubleAt(index: Int): Double

@SymbolName("Kotlin_ByteArray_setCharAt")
external fun ByteArray.setCharAt(index: Int, value: Char)

@SymbolName("Kotlin_ByteArray_setShortAt")
external fun ByteArray.setShortAt(index: Int, value: Short)

@SymbolName("Kotlin_ByteArray_setIntAt")
external fun ByteArray.setIntAt(index: Int, value: Int)

@SymbolName("Kotlin_ByteArray_setLongAt")
external fun ByteArray.setLongAt(index: Int, value: Long)

@SymbolName("Kotlin_ByteArray_setFloatAt")
external fun ByteArray.setFloatAt(index: Int, value: Float)

@SymbolName("Kotlin_ByteArray_setDoubleAt")
external fun ByteArray.setDoubleAt(index: Int, value: Double)
