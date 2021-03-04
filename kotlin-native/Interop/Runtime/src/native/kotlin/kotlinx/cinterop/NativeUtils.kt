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

package kotlinx.cinterop

import kotlin.native.internal.Intrinsic
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType

internal fun encodeToUtf8(str: String): ByteArray = str.encodeToByteArray()

@SymbolName("Kotlin_CString_toKStringFromUtf8Impl")
internal external fun CPointer<ByteVar>.toKStringFromUtf8Impl(): String

@TypedIntrinsic(IntrinsicType.INTEROP_BITS_TO_FLOAT)
external fun bitsToFloat(bits: Int): Float

@TypedIntrinsic(IntrinsicType.INTEROP_BITS_TO_DOUBLE)
external fun bitsToDouble(bits: Long): Double

// TODO: deprecate.
@TypedIntrinsic(IntrinsicType.INTEROP_SIGN_EXTEND)
external inline fun <reified R : Number> Number.signExtend(): R

// TODO: deprecate.
@TypedIntrinsic(IntrinsicType.INTEROP_NARROW)
external inline fun <reified R : Number> Number.narrow(): R

@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> Byte.convert(): R
@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> Short.convert(): R
@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> Int.convert(): R
@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> Long.convert(): R
@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> UByte.convert(): R
@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> UShort.convert(): R
@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> UInt.convert(): R
@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> ULong.convert(): R

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
internal annotation class JvmName(val name: String)

fun cValuesOf(vararg elements: UByte): CValues<UByteVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

fun cValuesOf(vararg elements: UShort): CValues<UShortVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

fun cValuesOf(vararg elements: UInt): CValues<UIntVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

fun cValuesOf(vararg elements: ULong): CValues<ULongVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

fun UByteArray.toCValues() = cValuesOf(*this)
fun UShortArray.toCValues() = cValuesOf(*this)
fun UIntArray.toCValues() = cValuesOf(*this)
fun ULongArray.toCValues() = cValuesOf(*this)