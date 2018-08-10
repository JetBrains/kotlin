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

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.NativePointed
import kotlinx.cinterop.NativePtr

@Intrinsic @PublishedApi external internal fun areEqualByValue(first: Boolean, second: Boolean): Boolean
@Intrinsic @PublishedApi external internal fun areEqualByValue(first: Byte, second: Byte): Boolean
@Intrinsic @PublishedApi external internal fun areEqualByValue(first: Short, second: Short): Boolean
@Intrinsic @PublishedApi external internal fun areEqualByValue(first: Int, second: Int): Boolean
@Intrinsic @PublishedApi external internal fun areEqualByValue(first: Long, second: Long): Boolean
@Intrinsic @PublishedApi external internal fun areEqualByValue(first: NativePtr, second: NativePtr): Boolean

// Bitwise equality:
@Intrinsic @PublishedApi external internal fun areEqualByValue(first: Float, second: Float): Boolean
@Intrinsic @PublishedApi external internal fun areEqualByValue(first: Double, second: Double): Boolean

// IEEE754 equality:
@Intrinsic @PublishedApi external internal fun ieee754Equals(first: Float, second: Float): Boolean
@Intrinsic @PublishedApi external internal fun ieee754Equals(first: Double, second: Double): Boolean

// Reinterprets this value from T to R having the same binary representation (e.g. to unwrap inline class).
@Intrinsic @PublishedApi external internal fun <T, R> T.reinterpret(): R
