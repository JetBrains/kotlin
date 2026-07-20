/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.konan.NativeBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal class ReifiedFunctionLowering(private val backendContext: NativeBackendContext) : FileLoweringPass, IrElementTransformerVoid() {
    companion object {
        val IrSimpleFunction.isReifiedInline: Boolean
            get() = isInline && typeParameters.any { it.isReified }
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid()
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (declaration.isFakeOverride || !declaration.isReifiedInline || declaration.body == null) return visitDeclaration(declaration)

        val builder = backendContext.createIrBuilder(backendContext.symbols.throwIllegalStateExceptionWithMessage)
                .at(declaration)

        fun IrBuilderWithScope.throwException(): IrExpression {
            return irThrow(
                    irCall(backendContext.symbols.throwIllegalStateExceptionWithMessage.owner).apply {
                        arguments[0] = "unsupported call of reified inlined function `${declaration.fqNameForIrSerialization}`"
                                .toIrConst(backendContext.irBuiltIns.stringType)
                    }
            )
        }

        declaration.body = builder.irBlockBody {
            +builder.throwException()
        }

        // We also want to replace default arguments in case they are using reified parameters
        for (parameter in declaration.parameters) {
            if (parameter.defaultValue != null) {
                parameter.defaultValue = builder.irExprBody(builder.throwException())
            }
        }
        return declaration
    }
}