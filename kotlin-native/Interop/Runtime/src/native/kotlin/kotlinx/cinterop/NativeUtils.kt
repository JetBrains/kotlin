/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.cinterop

import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.Intrinsic
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType

internal fun encodeToUtf8(str: String): ByteArray = str.encodeToByteArray()

@GCUnsafeCall("Kotlin_CString_toKStringFromUtf8Impl")
@ExperimentalForeignApi
internal external fun CPointer<ByteVar>.toKStringFromUtf8Impl(): String

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_BITS_TO_FLOAT)
external fun bitsToFloat(bits: Int): Float

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_BITS_TO_DOUBLE)
external fun bitsToDouble(bits: Long): Double

@Deprecated("Deprecated without replacement as part of the obsolete interop API", level = DeprecationLevel.WARNING)
@TypedIntrinsic(IntrinsicType.INTEROP_SIGN_EXTEND)
external inline fun <reified R : Number> Number.signExtend(): R

@Deprecated("Deprecated without replacement as part of the obsolete interop API", level = DeprecationLevel.WARNING)
@TypedIntrinsic(IntrinsicType.INTEROP_NARROW)
external inline fun <reified R : Number> Number.narrow(): R

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> Byte.convert(): R
@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> Short.convert(): R
@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> Int.convert(): R
@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> Long.convert(): R
@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> UByte.convert(): R
@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> UShort.convert(): R
@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> UInt.convert(): R
@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_CONVERT) external inline fun <reified R : Any> ULong.convert(): R

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
internal annotation class JvmName(val name: String)

@ExperimentalForeignApi
fun cValuesOf(vararg elements: UByte): CValues<UByteVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

@ExperimentalForeignApi
fun cValuesOf(vararg elements: UShort): CValues<UShortVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

@ExperimentalForeignApi
fun cValuesOf(vararg elements: UInt): CValues<UIntVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

@ExperimentalForeignApi
fun cValuesOf(vararg elements: ULong): CValues<ULongVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

@ExperimentalForeignApi
fun UByteArray.toCValues() = cValuesOf(*this)
@ExperimentalForeignApi
fun UShortArray.toCValues() = cValuesOf(*this)
@ExperimentalForeignApi
fun UIntArray.toCValues() = cValuesOf(*this)
@ExperimentalForeignApi
fun ULongArray.toCValues() = cValuesOf(*this)
