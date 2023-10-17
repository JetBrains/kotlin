/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.embeddings.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.names.ReturnLabelName
import org.jetbrains.kotlin.formver.names.ReturnVariableName
import org.jetbrains.kotlin.formver.names.embedName
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.Name

/**
 * Name resolver for parameters and return values and labels.
 *
 * Since parameter names may map to lambda embeddings, we use `embed` for those for consistency.
 */
interface ParameterResolver {
    fun tryEmbedParameter(symbol: FirValueParameterSymbol): ExpEmbedding?

    val resolvedReturnVarName: MangledName
    val resolvedReturnLabelName: ReturnLabelName
}

class RootParameterResolver(
    val ctx: ProgramConversionContext,
    override val resolvedReturnLabelName: ReturnLabelName,
) : ParameterResolver {
    override fun tryEmbedParameter(symbol: FirValueParameterSymbol): ExpEmbedding =
        VariableEmbedding(symbol.embedName(), ctx.embedType(symbol.resolvedReturnType), symbol.source)

    override val resolvedReturnVarName: MangledName = ReturnVariableName
}

class InlineParameterResolver(
    override val resolvedReturnVarName: MangledName,
    override val resolvedReturnLabelName: ReturnLabelName,
    private val substitutions: Map<Name, ExpEmbedding>,
) : ParameterResolver {
    override fun tryEmbedParameter(symbol: FirValueParameterSymbol): ExpEmbedding? = substitutions[symbol.name]
}