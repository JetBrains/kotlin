/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.internal.ActualizeByJvmBuiltinProvider

/**
 * The `String` class represents character strings. All string literals in Kotlin programs, such as `"abc"`, are
 * implemented as instances of this class.
 */
@ActualizeByJvmBuiltinProvider
public expect class String : Comparable<String>, CharSequence {
    public companion object {}

    /**
     * Returns a string obtained by concatenating this string with the string representation of the given [other] object.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public operator fun plus(other: Any?): String

    @kotlin.internal.IntrinsicConstEvaluation
    public override val length: Int

    /**
     * Returns the character of this string at the specified [index].
     *
     * If the [index] is out of bounds of this string, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public override fun get(index: Int): Char

    public override fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    @kotlin.internal.IntrinsicConstEvaluation
    public override fun compareTo(other: String): Int

    @kotlin.internal.IntrinsicConstEvaluation
    public override fun equals(other: Any?): Boolean

    @kotlin.internal.IntrinsicConstEvaluation
    public override fun toString(): String
}
