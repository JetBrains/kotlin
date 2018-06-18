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

@ExportTypeInfo("theStringTypeInfo")
@konan.internal.Frozen
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

    @SymbolName("Kotlin_String_get")
    external override public fun get(index: Int): Char

    @SymbolName("Kotlin_String_subSequence")
    external override public fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    @SymbolName("Kotlin_String_compareTo")
    override external public fun compareTo(other: String): Int

    @SymbolName("Kotlin_String_getStringLength")
    external private fun getStringLength(): Int

    @SymbolName("Kotlin_String_plusImpl")
    external private fun plusImpl(other: Any): String

    @SymbolName("Kotlin_String_equals")
    external public override fun equals(other: Any?): Boolean
}

// TODO: in big Kotlin this operations are in kotlin.kotlin_builtins.
private fun nullString() = "null"

public operator fun kotlin.String?.plus(other: kotlin.Any?): kotlin.String =
    (this?.toString() ?: nullString()).plus(other?.toString() ?: nullString())


public fun Any?.toString() = this?.toString() ?: nullString()