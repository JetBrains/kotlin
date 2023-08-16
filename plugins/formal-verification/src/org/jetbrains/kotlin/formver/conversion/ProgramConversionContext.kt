/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.formver.embeddings.MethodSignatureEmbedding
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding

interface ProgramConversionContext {
    fun add(symbol: FirNamedFunctionSymbol): MethodSignatureEmbedding
    fun embedType(type: ConeKotlinType): TypeEmbedding
}