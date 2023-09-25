/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.embeddings.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Label
import org.jetbrains.kotlin.name.Name

interface MethodConversionContext : ProgramConversionContext {
    val signature: FunctionSignature
    val resolvedReturnVarName: MangledName
    val resolvedReturnLabelName: ReturnLabelName

    fun embedParameter(symbol: FirValueParameterSymbol): ExpEmbedding
    fun resolveLocalPropertyName(name: Name): MangledName
    fun registerLocalPropertyName(name: Name)

    fun <R> withScopeImpl(scopeDepth: Int, action: () -> R): R
}

fun MethodConversionContext.embedLocalProperty(symbol: FirPropertySymbol): VariableEmbedding =
    VariableEmbedding(resolveLocalPropertyName(symbol.name), embedType(symbol.resolvedReturnType))

fun MethodConversionContext.embedLocalSymbol(symbol: FirBasedSymbol<*>): ExpEmbedding =
    when (symbol) {
        is FirValueParameterSymbol -> embedParameter(symbol)
        is FirPropertySymbol -> embedLocalProperty(symbol)
        else -> throw IllegalArgumentException("Symbol $symbol cannot be embedded as a local symbol.")
    }

val MethodConversionContext.returnVar: VariableEmbedding
    get() = VariableEmbedding(resolvedReturnVarName, signature.returnType)

// It seems like Viper will propagate the weakest precondition through the label correctly even in the absence of
// explicit invariants; we only need to add those if we want to make a stronger claim.
val MethodConversionContext.returnLabel: Label
    get() = Label(resolvedReturnLabelName, listOf())
