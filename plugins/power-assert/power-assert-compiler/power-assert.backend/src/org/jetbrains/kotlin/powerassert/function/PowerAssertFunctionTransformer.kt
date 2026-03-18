/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class PowerAssertFunctionTransformer(
    builtIns: PowerAssertBuiltIns,
    private val factory: PowerAssertFunctionFactory,
) : DeclarationTransformer {
    private val transformer = PowerAssertGetExplanationTransformer(builtIns, parameter = null)

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction && declaration.hasAnnotationOrOverridden(PowerAssertBuiltIns.powerAssertClassId)) {
            return lower(declaration)
        }

        return null
    }

    private fun lower(originalFunction: IrSimpleFunction): List<IrSimpleFunction> {
        val powerAssertFunction = factory.generate(originalFunction)

        // Transform the original function to use `null` instead of PowerAssert.explanation.
        // This keeps the code from throwing an error when PowerAssert.explanation is used.
        // This in turn helps make sure the compiler-plugin is applied to functions which use `@PowerAssert`.
        originalFunction.transformChildrenVoid(transformer)

        return listOf(originalFunction, powerAssertFunction)
    }
}
