/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir.annotations

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.util.allParameters
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.name.NativeRuntimeNames

/**
 * Kind in `@PointsTo` annotation.
 *  kind            edge
 *   0      p1          -/-> p2
 *   1      p1            -> p2
 *   2      p1            -> p2.intestines
 *   3      p1.intestines -> p2
 *   4      p1.intestines -> p2.intestines
 */
@JvmInline
internal value class PointsToKind private constructor(private val value: Int) {
    val sourceIsDirect: Boolean
        get() = value < 3

    val destinationIsDirect: Boolean
        get() = value % 2 == 1

    companion object {
        fun fromMask(mask: Int): PointsToKind? {
            require(mask >= 0 && mask <= 4) {
                "$mask must be 0..4"
            }
            return if (mask == 0) null else PointsToKind(mask)
        }
    }
}

/**
 * A representation of `@PointsTo` annotation.
 */
@JvmInline
internal value class PointsTo private constructor(private val elements: IntArray) {
    constructor(elements: List<Int>, signatureSize: Int) : this(elements.toIntArray()) {
        assertIsValidFor(signatureSize)
    }

    /**
     * Throws [IllegalArgumentException] if `this` is not valid for signature of size [signatureSize].
     */
    fun assertIsValidFor(signatureSize: Int) {
        require(elements.size == signatureSize) {
            "$this must have exactly $signatureSize elements"
        }
        elements.forEach {
            require(it >= 0 && it shr (4 * signatureSize) == 0) {
                "0x${it.toString(16)} must not be negative and not have nibbles higher than $signatureSize"
            }
        }
    }

    /**
     * If signature element at [indexFrom] points to signature element at [indexTo], returns [PointsToKind]
     * of the relationship. Otherwise, returns `null`.
     */
    fun kind(indexFrom: Int, indexTo: Int): PointsToKind? {
        val mask = elements[indexFrom] shr (4 * indexTo) and 15
        return PointsToKind.fromMask(mask)
    }

    override fun toString(): String = elements.joinToString(prefix = "(", postfix = ")", separator = ", ") {
        "0x${it.toString(16)}"
    }
}

/**
 * Get `@PointsTo` signature for the function if any.
 */
internal val IrFunction.pointsTo: PointsTo?
    get() = annotations.findAnnotation(NativeRuntimeNames.Annotations.PointsTo.asSingleFqName())?.run {
        PointsTo((arguments[0]!! as IrVararg).elements.map { (it as IrConst).value as Int }, allParameters.size + 1)
    }