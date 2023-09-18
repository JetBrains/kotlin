/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.conversion.getFunctionCallSubstitutionItems
import org.jetbrains.kotlin.formver.embeddings.callables.CallableEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.embeddings.callables.asData
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.name.Name

class LambdaExp(
    val signature: FunctionSignature, val function: FirAnonymousFunction,
    val scopedNames: Map<Name, Int>,
) : CallableEmbedding, ExpEmbedding,
    FunctionSignature by signature {
    override val type: TypeEmbedding = FunctionTypeEmbedding(signature.asData)

    override fun toViper(): Exp = TODO("create new function object with counter, duplicable (requires toViper restructuring)")

    override fun insertCallImpl(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        ctx.withResult(returnType) {
            val paramNames = function.valueParameters.map { it.name }
            paramNames.forEach { ctx.addScopedName(it) }
            val callArgs = ctx.getFunctionCallSubstitutionItems(args)
            val subs = paramNames.zip(callArgs).toMap()
            val lambdaCtx = this.newBlock().withLambdaContext(this.signature, this.resultCtx.resultVar.name, subs, scopedNames)
            lambdaCtx.convert(function.body!!)
            // NOTE: It is necessary to drop the last stmt because is a wrong goto
            val sqn = lambdaCtx.block.copy(stmts = lambdaCtx.block.stmts.dropLast(1))
            // NOTE: Putting the block inside the then branch of an if-true statement is a little hack to make Viper respect the scoping
            addStatement(Stmt.If(Exp.BoolLit(true), sqn, Stmt.Seqn(listOf(), listOf())))
        }
}