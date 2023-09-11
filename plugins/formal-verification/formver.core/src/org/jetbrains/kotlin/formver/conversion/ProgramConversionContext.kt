/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.formver.PluginConfiguration
import org.jetbrains.kotlin.formver.embeddings.MethodEmbedding
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding

interface ProgramConversionContext {
    val config: PluginConfiguration

    fun embedFunction(symbol: FirFunctionSymbol<*>): MethodEmbedding
    fun embedType(type: ConeKotlinType): TypeEmbedding
    fun embedType(exp: FirExpression): TypeEmbedding = embedType(exp.resolvedType)
    fun newAnonName(): AnonymousName
    fun newWhileIndex(): Int
}

fun ProgramConversionContext.newAnonVar(type: TypeEmbedding): VariableEmbedding = VariableEmbedding(newAnonName(), type)
