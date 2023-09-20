/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.conversion.getFunctionCallSubstitutionItems
import org.jetbrains.kotlin.formver.conversion.returnLabel
import org.jetbrains.kotlin.formver.embeddings.ExpEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

class InlineNamedFunction(
    val signature: FullNamedFunctionSignature,
    val symbol: FirFunctionSymbol<*>,
) : CallableEmbedding, FullNamedFunctionSignature by signature {
    @OptIn(SymbolInternals::class)
    override fun insertFirCallImpl(firArgs: List<FirExpression>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        ctx.withResult(returnType) {
            val inlineBody = symbol.fir.body ?: throw Exception("Function symbol $symbol has a null body")
            val inlineBodyCtx = newBlock()
            val inlineArgs = symbol.valueParameterSymbols.map { it.name }
            val callArgs = inlineBodyCtx.getFunctionCallSubstitutionItems(firArgs)
            val substitutionParams = inlineArgs.zip(callArgs).toMap()

            val inlineCtx = inlineBodyCtx.withInlineContext(
                this@InlineNamedFunction.signature,
                inlineBodyCtx.resultCtx.resultVar.name,
                substitutionParams
            )
            inlineCtx.convert(inlineBody)
            // TODO: add these labels automatically.
            inlineCtx.addDeclaration(inlineCtx.returnLabel.toDecl())
            inlineCtx.addStatement(inlineCtx.returnLabel.toStmt())
            // Note: Putting the block inside the then branch of an if-true statement is a little hack to make Viper respect the scoping
            addStatement(Stmt.If(Exp.BoolLit(true), inlineCtx.block, Stmt.Seqn(listOf(), listOf())))
        }

    override fun insertCallImpl(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        TODO("InlineUserFunction must be called with the FIR; WIP to do this correctly.")
    }
}