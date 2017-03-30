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

package kotlin

/**
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
@SymbolName("Kotlin_Double_isNaN")
external public fun Double.isNaN(): Boolean

/**
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
@SymbolName("Kotlin_Float_isNaN")
external public fun Float.isNaN(): Boolean

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
@SymbolName("Kotlin_Double_isInfinite")
external public fun Double.isInfinite(): Boolean

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
@SymbolName("Kotlin_Float_isInfinite")
external public fun Float.isInfinite(): Boolean

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
@SymbolName("Kotlin_Double_isFinite")
external public fun Double.isFinite(): Boolean

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
@SymbolName("Kotlin_Float_isFinite")
external public fun Float.isFinite(): Boolean
