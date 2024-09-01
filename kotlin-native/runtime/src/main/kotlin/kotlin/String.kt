/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.native.internal.ExportTypeInfo
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.IntrinsicType
import kotlin.native.internal.TypedIntrinsic

@ExportTypeInfo("theStringTypeInfo")
public actual class String : Comparable<String>, CharSequence {
    public actual companion object {
    }

    @GCUnsafeCall("Kotlin_String_hashCode")
    public external override fun hashCode(): Int

    @kotlin.internal.IntrinsicConstEvaluation
    public actual operator fun plus(other: Any?): String {
        return plusImpl(other.toString())
    }

    @TypedIntrinsic(IntrinsicType.IDENTITY)
    @kotlin.internal.IntrinsicConstEvaluation
    public actual override fun toString(): String {
        return this
    }

    @kotlin.internal.IntrinsicConstEvaluation
    public actual override val length: Int
        get() = getStringLength()

    /**
     * Returns the character of this string at the specified [index].
     *
     * If the [index] is out of bounds of this string, throws an [IndexOutOfBoundsException].
     */
    @GCUnsafeCall("Kotlin_String_get")
    @kotlin.internal.IntrinsicConstEvaluation
    public actual external override fun get(index: Int): Char

    @GCUnsafeCall("Kotlin_String_subSequence")
    public actual external override fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    @GCUnsafeCall("Kotlin_String_compareTo")
    @kotlin.internal.IntrinsicConstEvaluation
    public actual external override fun compareTo(other: String): Int

    @GCUnsafeCall("Kotlin_String_getStringLength")
    private external fun getStringLength(): Int

    @PublishedApi
    @GCUnsafeCall("Kotlin_String_plusImpl")
    internal external fun plusImpl(other: String): String

    @GCUnsafeCall("Kotlin_String_equals")
    @kotlin.internal.IntrinsicConstEvaluation
    actual external override fun equals(other: Any?): Boolean
}

@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun kotlin.String?.plus(other: kotlin.Any?): kotlin.String =
    (this?.toString() ?: "null").plus(other?.toString() ?: "null")

@Suppress("NOTHING_TO_INLINE")
public actual inline fun Any?.toString(): String = this?.toString() ?: "null"