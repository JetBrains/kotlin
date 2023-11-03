/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.formver.conversion.MethodConversionContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.conversion.insertInlineFunctionCall
import org.jetbrains.kotlin.formver.embeddings.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.CallableEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.embeddings.callables.asData
import org.jetbrains.kotlin.formver.embeddings.expression.debug.PlaintextLeaf
import org.jetbrains.kotlin.formver.embeddings.expression.debug.TreeView
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.ast.Exp

class LambdaExp(
    val signature: FunctionSignature,
    val function: FirAnonymousFunction,
    private val parentCtx: MethodConversionContext,
) : CallableEmbedding, StoredResultExpEmbedding,
    FunctionSignature by signature {
    override val type: TypeEmbedding = FunctionTypeEmbedding(signature.asData)

    override fun toViperStoringIn(result: Exp.LocalVar, ctx: LinearizationContext) {
        TODO("create new function object with counter, duplicable (requires toViper restructuring)")
    }

    override fun insertCallImpl(
        args: List<ExpEmbedding>,
        ctx: StmtConversionContext,
    ): ExpEmbedding {
        val inlineBody = function.body ?: throw IllegalArgumentException("Lambda $function has a null body")
        val paramNames = function.valueParameters.map { it.name }
        return ctx.insertInlineFunctionCall(signature, paramNames, args, inlineBody, ctx.signature.sourceName, parentCtx)
    }

    override val debugTreeView: TreeView
        get() = PlaintextLeaf("Lambda")
}