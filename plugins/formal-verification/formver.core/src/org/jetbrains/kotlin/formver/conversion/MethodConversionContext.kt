/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.embeddings.MethodEmbedding
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Label

interface MethodConversionContext : ProgramConversionContext {
    val method: MethodEmbedding
    val returnLabel: Label
    val returnVar: VariableEmbedding

    fun resolveName(name: MangledName): MangledName

    fun getVariableEmbedding(name: MangledName, type: TypeEmbedding): VariableEmbedding =
        VariableEmbedding(resolveName(name), type)

    fun getLambdaOrNull(name: MangledName): SubstitutionLambda? = null
}
