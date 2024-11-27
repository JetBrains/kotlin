/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.error
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * These intrinsics must be lowered separately from the interop lowering
 * as their proper handling requires inlining phase to have been applied.
 */
internal class SpecialInteropIntrinsicsLowering(val context: Context) : FileLoweringPass {
    private val symbols = context.symbols

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrBuildingTransformer(context) {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid()

                val callee = expression.symbol.owner
                return when (callee.symbol) {
                    symbols.interopTypeOf -> {
                        val typeArgument = expression.getTypeArgument(0)!!
                        val classSymbol = typeArgument.classifierOrNull as? IrClassSymbol

                        if (classSymbol == null) {
                            expression
                        } else {
                            val irClass = classSymbol.owner

                            val companionObject = irClass.companionObject()
                                    ?: error(
                                            irFile,
                                            expression,
                                            "native variable class ${irClass.render()} must have the companion object"
                                    )

                            builder.at(expression).irGetObject(companionObject.symbol)
                        }
                    }
                    else -> expression
                }
            }
        })
    }
}
