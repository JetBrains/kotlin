/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ErrorReportingContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.getCompilerMessageLocation
import org.jetbrains.kotlin.backend.common.ir.FrontendSymbols
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrTransformer

internal class TypeOfProcessingLowering(val generationState: NativeGenerationState) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        Transformer(generationState.context.symbols, generationState.context).visitFile(irFile, null)
    }
}

private class Transformer(private val symbols: KonanSymbols, private val errorContext: ErrorReportingContext) : IrTransformer<IrDeclaration?>() {
    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration?): IrStatement {
        return super.visitDeclaration(declaration, declaration)
    }

    override fun visitCall(expression: IrCall, data: IrDeclaration?): IrElement {
        if (FrontendSymbols.isTypeOfIntrinsic(expression.symbol)) {
            val symbol = data?.symbol ?: error("\"typeOf\" call in unexpected position")
            val builder = symbols.irBuiltIns
                    .createIrBuilder(symbol, expression.startOffset, expression.endOffset)
                    .toNativeRuntimeReflectionBuilder(symbols) { message ->
                        errorContext.reportCompilationError(message, expression.getCompilerMessageLocation(data.file))
                    }
            return builder.irKType(expression.typeArguments[0]!!)
        }
        return super.visitCall(expression, data)
    }
}
