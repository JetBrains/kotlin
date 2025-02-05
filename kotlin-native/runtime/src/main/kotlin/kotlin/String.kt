/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.native.internal.ExportTypeInfo
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.IntrinsicType
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.escapeAnalysis.Escapes

@ExportTypeInfo("theStringTypeInfo")
public actual class String : Comparable<String>, CharSequence {
    public actual companion object {
    }

    @GCUnsafeCall("Kotlin_String_hashCode")
    @Escapes.Nothing
    public external override fun hashCode(): Int

    /**
     * Returns a string obtained by concatenating this string with the string representation of the given [other] object.
     *
     * @sample samples.text.Strings.stringPlus
     */
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
    @Escapes.Nothing
    public actual external override fun get(index: Int): Char

    @GCUnsafeCall("Kotlin_String_subSequence")
    // The return value may be an empty string, which is statically allocated and immutable;
    // we can treat it as non-escaping
    @Escapes.Nothing
    public actual external override fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    @GCUnsafeCall("Kotlin_String_compareTo")
    @kotlin.internal.IntrinsicConstEvaluation
    @Escapes.Nothing
    public actual external override fun compareTo(other: String): Int

    @GCUnsafeCall("Kotlin_String_getStringLength")
    @Escapes.Nothing
    private external fun getStringLength(): Int

    @PublishedApi
    @GCUnsafeCall("Kotlin_String_plusImpl")
    @Escapes.Nothing
    internal external fun plusImpl(other: String): String

    /**
     * Indicates if [other] object is equal to this [String].
     *
     * An [other] object is equal to this [String] if and only if it is also a [String],
     * it has the same [length] as this String,
     * and characters at the same positions in each string are equal to each other.
     *
     * @sample samples.text.Strings.stringEquals
     */
    @GCUnsafeCall("Kotlin_String_equals")
    @kotlin.internal.IntrinsicConstEvaluation
    @Escapes.Nothing
    actual external override fun equals(other: Any?): Boolean
}

@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun kotlin.String?.plus(other: kotlin.Any?): kotlin.String =
    (this?.toString() ?: "null").plus(other?.toString() ?: "null")

@Suppress("NOTHING_TO_INLINE")
public actual inline fun Any?.toString(): String = this?.toString() ?: "null"
