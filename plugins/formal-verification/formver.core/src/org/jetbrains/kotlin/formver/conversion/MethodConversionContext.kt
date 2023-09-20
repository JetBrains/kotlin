/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.FullNamedFunctionSignature
import org.jetbrains.kotlin.formver.viper.ast.Label
import org.jetbrains.kotlin.name.Name

interface MethodConversionContext : ProgramConversionContext {
    val signature: FullNamedFunctionSignature
    val nameMangler: NameMangler
    fun getLambdaOrNull(name: Name): SubstitutionLambda?
}

fun MethodConversionContext.embedValueParameter(symbol: FirValueParameterSymbol): VariableEmbedding =
    VariableEmbedding(
        // Parameters always have scope depth equal to zero
        nameMangler.mangleParameterName(symbol),
        embedType(symbol.resolvedReturnType)
    )

fun MethodConversionContext.embedLocalProperty(symbol: FirPropertySymbol, scopeDepth: Int): VariableEmbedding =
    VariableEmbedding(
        nameMangler.mangleLocalPropertyName(
            symbol,
            scopeDepth
        ), embedType(symbol.resolvedReturnType)
    )

val MethodConversionContext.returnVar: VariableEmbedding
    get() = VariableEmbedding(nameMangler.mangledReturnValueName, signature.returnType)

// It seems like Viper will propagate the weakest precondition through the label correctly even in the absence of
// explicit invariants; we only need to add those if we want to make a stronger claim.
val MethodConversionContext.returnLabel: Label
    get() = Label(nameMangler.mangledReturnLabelName, listOf())
