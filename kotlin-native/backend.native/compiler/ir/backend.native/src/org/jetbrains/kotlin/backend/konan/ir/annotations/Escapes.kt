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
import org.jetbrains.kotlin.native.internal.Escapes

/**
 * Get `@Escapes` signature for the function if any.
 */
internal val IrFunction.escapes: Escapes?
    get() = annotations.findAnnotation(NativeRuntimeNames.Annotations.Escapes.asSingleFqName())?.run {
        Escapes((arguments[0]!! as IrConst).value as Int, allParameters.size + 1)
    } ?: annotations.findAnnotation(EscapesNothing.asSingleFqName())?.let { Escapes(0, allParameters.size + 1) }