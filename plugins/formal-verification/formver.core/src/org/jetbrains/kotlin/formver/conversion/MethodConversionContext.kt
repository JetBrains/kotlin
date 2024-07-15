/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.PlaceholderVariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.names.ReturnLabelName
import org.jetbrains.kotlin.formver.names.ReturnVariableName
import org.jetbrains.kotlin.formver.viper.ast.Label
import org.jetbrains.kotlin.name.Name

class ReturnTarget(depth: Int, type: TypeEmbedding) {
    val variable = PlaceholderVariableEmbedding(ReturnVariableName(depth), type)
    val label = Label(ReturnLabelName(depth), listOf())
}

/**
 * Context for converting a method body.
 *
 * We use the terms `register`, `resolve`, and `embed` a lot here. For consistency:
 * - `register` takes a name or symbol and an embedding and stores it.
 * - `resolve` takes a name and retrieves an already-existing embedding.
 * - `embed` takes a symbol and returns an embedding; this embedding may be existing or new.
 */
interface MethodConversionContext : ProgramConversionContext {
    val signature: FunctionSignature
    val defaultResolvedReturnTarget: ReturnTarget

    fun resolveParameter(name: Name): ExpEmbedding
    fun resolveLocal(name: Name): VariableEmbedding
    fun registerLocalProperty(symbol: FirPropertySymbol)
    fun registerLocalVariable(symbol: FirVariableSymbol<*>)
    fun resolveReceiver(): ExpEmbedding?

    fun <R> withScopeImpl(scopeDepth: Int, action: () -> R): R
    fun addLoopIdentifier(labelName: String, index: Int)
    fun resolveLoopIndex(name: String): Int
    fun resolveNamedReturnTarget(sourceName: String): ReturnTarget?
}

fun MethodConversionContext.resolveReturnTarget(targetSourceName: String?): ReturnTarget =
    if (targetSourceName == null) defaultResolvedReturnTarget
    else resolveNamedReturnTarget(targetSourceName) ?: throw IllegalArgumentException("Cannot resolve returnTarget of $targetSourceName")

fun MethodConversionContext.embedLocalProperty(symbol: FirPropertySymbol): VariableEmbedding = resolveLocal(symbol.name)
fun MethodConversionContext.embedParameter(symbol: FirValueParameterSymbol): ExpEmbedding = resolveParameter(symbol.name)
fun MethodConversionContext.embedLocalVariable(symbol: FirVariableSymbol<*>): VariableEmbedding = resolveLocal(symbol.name)

fun MethodConversionContext.embedLocalSymbol(symbol: FirBasedSymbol<*>): ExpEmbedding =
    when (symbol) {
        is FirValueParameterSymbol -> embedParameter(symbol)
        is FirPropertySymbol -> embedLocalProperty(symbol)
        is FirVariableSymbol<*> -> embedLocalVariable(symbol)
        else -> throw IllegalArgumentException("Symbol $symbol cannot be embedded as a local symbol.")
    }
