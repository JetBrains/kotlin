/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.names.ReturnLabelName
import org.jetbrains.kotlin.formver.names.ReturnVariableName
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Label
import org.jetbrains.kotlin.name.Name

class ReturnTarget(depth: Int, type: TypeEmbedding) {
    val variable = VariableEmbedding(ReturnVariableName(depth), type)
    val label = Label(ReturnLabelName(depth), listOf())
}

interface MethodConversionContext : ProgramConversionContext {
    val signature: FunctionSignature
    val defaultResolvedReturnTarget: ReturnTarget

    fun embedParameter(symbol: FirValueParameterSymbol): ExpEmbedding
    fun resolveLocalPropertyName(name: Name): MangledName
    fun registerLocalPropertyName(name: Name)

    fun <R> withScopeImpl(scopeDepth: Int, action: () -> R): R
    fun addLoopIdentifier(labelName: String, index: Int)
    fun resolveLoopIndex(name: String): Int
    fun resolveNamedReturnTarget(sourceName: String): ReturnTarget?
}

fun MethodConversionContext.resolveReturnTarget(targetSourceName: String?): ReturnTarget =
    if (targetSourceName == null) defaultResolvedReturnTarget
    else resolveNamedReturnTarget(targetSourceName) ?: throw IllegalArgumentException("Cannot resolve returnTarget of $targetSourceName")

fun MethodConversionContext.embedLocalProperty(symbol: FirPropertySymbol): VariableEmbedding =
    VariableEmbedding(resolveLocalPropertyName(symbol.name), embedType(symbol.resolvedReturnType))

fun MethodConversionContext.embedLocalSymbol(symbol: FirBasedSymbol<*>): ExpEmbedding =
    when (symbol) {
        is FirValueParameterSymbol -> embedParameter(symbol)
        is FirPropertySymbol -> embedLocalProperty(symbol)
        else -> throw IllegalArgumentException("Symbol $symbol cannot be embedded as a local symbol.")
    }
