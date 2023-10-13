/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.formver.ErrorCollector
import org.jetbrains.kotlin.formver.PluginConfiguration
import org.jetbrains.kotlin.formver.embeddings.PropertyEmbedding
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.names.AnonymousName
import org.jetbrains.kotlin.formver.names.CatchLabelName
import org.jetbrains.kotlin.formver.names.ReturnLabelName
import org.jetbrains.kotlin.formver.names.TryExitLabelName

interface ProgramConversionContext {
    val config: PluginConfiguration
    val errorCollector: ErrorCollector

    val anonNameProducer: FreshEntityProducer<AnonymousName>
    val whileIndexProducer: FreshEntityProducer<Int>
    val returnLabelNameProducer: FreshEntityProducer<ReturnLabelName>
    val catchLabelNameProducer: FreshEntityProducer<CatchLabelName>
    val tryExitLabelNameProducer: FreshEntityProducer<TryExitLabelName>

    fun embedFunction(symbol: FirFunctionSymbol<*>): FunctionEmbedding
    fun embedFunctionSignature(symbol: FirFunctionSymbol<*>): FunctionSignature
    fun embedType(type: ConeKotlinType): TypeEmbedding
    fun embedType(symbol: FirFunctionSymbol<*>): TypeEmbedding
    fun embedType(exp: FirExpression): TypeEmbedding = embedType(exp.resolvedType)
    fun embedProperty(symbol: FirPropertySymbol): PropertyEmbedding
}

fun ProgramConversionContext.freshAnonVar(type: TypeEmbedding): VariableEmbedding = VariableEmbedding(anonNameProducer.getFresh(), type)
