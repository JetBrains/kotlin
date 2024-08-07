/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.name.NativeRuntimeNames.Annotations.Escapes
import org.jetbrains.kotlin.name.NativeRuntimeNames.Annotations.EscapesNothing
import org.jetbrains.kotlin.name.NativeRuntimeNames.Annotations.PointsTo

internal fun Int.isValidEscapesMask(signatureSize: Int): Boolean {
    if (this < 0) return false
    if (this shr signatureSize != 0) return false
    return true
}

internal fun Int.escapesAt(index: Int): Boolean {
    return (this shr index) and 1 == 1
}

internal val IrFunction.escapesMask: Int?
    get() = annotations.findAnnotation(Escapes.asSingleFqName())?.run {
        @Suppress("UNCHECKED_CAST") (getValueArgument(0)!! as IrConst<Int>).value
    } ?: annotations.findAnnotation(EscapesNothing.asSingleFqName())?.let { 0 }

internal fun Int.isValidPointsToElement(signatureSize: Int): Boolean {
    if (this < 0) return false
    if (this shr (4 * signatureSize) != 0) return false
    return true
}

internal fun Int.pointsToKindAt(index: Int): Int {
    return this shr (4 * index) and 15
}

internal val IrFunction.pointsToMasks: IntArray?
    get() = annotations.findAnnotation(PointsTo.asSingleFqName())?.run {
        @Suppress("UNCHECKED_CAST") (getValueArgument(0)!! as IrVararg).elements.map { (it as IrConst<Int>).value }.toIntArray()
    }