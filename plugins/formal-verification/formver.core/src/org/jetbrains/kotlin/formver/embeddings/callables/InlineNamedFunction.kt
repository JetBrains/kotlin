/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.conversion.insertInlineFunctionCall
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.VariableEmbedding

class InlineNamedFunction(
    val signature: FullNamedFunctionSignature,
    val symbol: FirFunctionSymbol<*>,
    val firBody: FirBlock,
) : RichCallableEmbedding, FullNamedFunctionSignature by signature {
    override fun insertCallImpl(
        args: List<ExpEmbedding>,
        ctx: StmtConversionContext,
    ): ExpEmbedding {
        val paramNames = symbol.valueParameterSymbols.map { it.name }
        return ctx.insertInlineFunctionCall(signature, paramNames, args, firBody, signature.sourceName)
    }

    override fun toViperMethodHeader(): Nothing? = null

    override val receiverType: TypeEmbedding?
        get() = signature.receiverType

    override val paramTypes: List<TypeEmbedding>
        get() = signature.paramTypes
}