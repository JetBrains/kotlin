/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir.annotations

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.allParameters
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.name.NativeRuntimeNames
import org.jetbrains.kotlin.name.NativeRuntimeNames.Annotations.EscapesNothing

/**
 * A representation of `@Escapes` and `@Escapes.Nothing` annotations.
 */
@JvmInline
internal value class Escapes private constructor(private val mask: Int) {
    constructor(mask: Int, signatureSize: Int) : this(mask) {
        assertIsValidFor(signatureSize)
    }

    /**
     * Throws [IllegalArgumentException] if `this` is not valid for signature of size [signatureSize].
     */
    fun assertIsValidFor(signatureSize: Int) {
        require(mask >= 0 && mask shr signatureSize == 0) {
            "$this must not be negative and not have bits higher than $signatureSize"
        }
    }

    /**
     * Returns `true` if a signature element at [index] is marked as escaping.
     */
    fun escapesAt(index: Int): Boolean {
        return (mask shr index) and 1 == 1
    }

    override fun toString(): String {
        return "0b${mask.toString(2)}"
    }
}

/**
 * Get `@Escapes` signature for the function if any.
 */
internal val IrFunction.escapes: Escapes?
    get() = annotations.findAnnotation(NativeRuntimeNames.Annotations.Escapes.asSingleFqName())?.run {
        Escapes((arguments[0]!! as IrConst).value as Int, allParameters.size + 1)
    } ?: annotations.findAnnotation(EscapesNothing.asSingleFqName())?.let { Escapes(0, allParameters.size + 1) }