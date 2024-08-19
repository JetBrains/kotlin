/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir.annotations

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.findAnnotation

import org.jetbrains.kotlin.name.NativeRuntimeNames

internal class ToRetainedSwift private constructor(
        val function: IrFunction,
        val targetClass: IrClass,
) {
    companion object {
        fun findInFunction(function: IrFunction): ToRetainedSwift? {
            val annotation = function.annotations.findAnnotation(NativeRuntimeNames.Annotations.ToRetainedSwift.asSingleFqName())
                    ?: return null
            check(annotation.valueArgumentsCount == 1)
            val arg = annotation.getValueArgument(0) as IrClassReference
            return ToRetainedSwift(function, arg.classType.getClass()!!)
        }
    }
}