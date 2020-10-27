/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

import kotlin.native.internal.ExportTypeInfo
import kotlin.native.internal.Frozen

@ExportTypeInfo("theStringTypeInfo")
@Frozen
public final class String : Comparable<String>, CharSequence {
    public companion object {
    }

    @SymbolName("Kotlin_String_hashCode")
    external public override fun hashCode(): Int

    public operator fun plus(other: Any?): String {
        return plusImpl(other.toString())
    }

    override public fun toString(): String {
        return this
    }

    public override val length: Int
        get() = getStringLength()

    /**
     * Returns the character of this string at the specified [index].
     *
     * If the [index] is out of bounds of this string, throws an [IndexOutOfBoundsException].
     */
    @SymbolName("Kotlin_String_get")
    external override public fun get(index: Int): Char

    @SymbolName("Kotlin_String_subSequence")
    external override public fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    @SymbolName("Kotlin_String_compareTo")
    override external public fun compareTo(other: String): Int

    @SymbolName("Kotlin_String_getStringLength")
    external private fun getStringLength(): Int

    @SymbolName("Kotlin_String_plusImpl")
    external private fun plusImpl(other: String): String

    @SymbolName("Kotlin_String_equals")
    external public override fun equals(other: Any?): Boolean
}

public inline operator fun kotlin.String?.plus(other: kotlin.Any?): kotlin.String =
    (this?.toString() ?: "null").plus(other?.toString() ?: "null")


public inline fun Any?.toString() = this?.toString() ?: "null"