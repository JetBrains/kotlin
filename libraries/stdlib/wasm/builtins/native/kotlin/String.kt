/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress(
    "NON_ABSTRACT_FUNCTION_WITH_NO_BODY",
    "MUST_BE_INITIALIZED_OR_BE_ABSTRACT",
    "EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE",
    "PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED",
    "WRONG_MODIFIER_TARGET"
)

package kotlin

import kotlin.wasm.internal.*
import kotlin.wasm.internal.ExcludedFromCodegen
import kotlin.wasm.internal.SkipRTTI
import kotlin.wasm.internal.WasmImport

/**
 * The `String` class represents character strings. All string literals in Kotlin programs, such as `"abc"`, are
 * implemented as instances of this class.
 */
@SkipRTTI
public class String @ExcludedFromCodegen constructor() : Comparable<String>, CharSequence {
    @ExcludedFromCodegen
    companion object {}
    
    /**
     * Returns a string obtained by concatenating this string with the string representation of the given [other] object.
     */
    @WasmImport("runtime", "String_plus")
    public operator fun plus(other: String): String

    public override val length: Int
        @WasmImport("runtime", "String_getLength") get

    /**
     * Returns the character of this string at the specified [index].
     *
     * If the [index] is out of bounds of this string, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    @WasmImport("runtime", "String_getChar")
    public override fun get(index: Int): Char

    @ExcludedFromCodegen
    public override fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    @WasmImport("runtime", "String_compareTo")
    public override fun compareTo(other: String): Int

    public override fun equals(other: Any?): Boolean =
        wasm_unreachable()

    public override fun toString(): String = this

    @ExcludedFromCodegen
    public override fun hashCode(): Int
}