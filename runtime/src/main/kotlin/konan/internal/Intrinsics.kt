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

@Intrinsic external fun areEqualByValue(first: Boolean, second: Boolean): Boolean
@Intrinsic external fun areEqualByValue(first: Char, second: Char): Boolean
@Intrinsic external fun areEqualByValue(first: Byte, second: Byte): Boolean
@Intrinsic external fun areEqualByValue(first: Short, second: Short): Boolean
@Intrinsic external fun areEqualByValue(first: Int, second: Int): Boolean
@Intrinsic external fun areEqualByValue(first: Long, second: Long): Boolean

@Intrinsic external fun ieee754Equals(first: Float, second: Float): Boolean
@Intrinsic external fun ieee754Equals(first: Double, second: Double): Boolean

@Intrinsic external fun areEqualByValue(first: NativePtr, second: NativePtr): Boolean
@Intrinsic external fun areEqualByValue(first: NativePointed?, second: NativePointed?): Boolean
@Intrinsic external fun areEqualByValue(first: CPointer<*>?, second: CPointer<*>?): Boolean