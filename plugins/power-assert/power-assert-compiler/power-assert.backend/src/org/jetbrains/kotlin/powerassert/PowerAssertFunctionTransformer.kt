/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation

class PowerAssertFunctionTransformer(
    private val builtIns: PowerAssertBuiltIns,
    private val factory: ExplainCallFunctionFactory,
) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction) {
            if (declaration.hasAnnotation(builtIns.explainCallClass)) {
                return lower(declaration)
            }
        }

        return null
    }

    private fun lower(irFunction: IrSimpleFunction): List<IrSimpleFunction> {
        return listOf(irFunction, factory.generate(irFunction))
    }
}
