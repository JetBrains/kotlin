/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.linearization.pureToViper
import org.jetbrains.kotlin.formver.viper.ast.*

interface FullNamedFunctionSignature : NamedFunctionSignature {
    fun getPreconditions(returnVariable: VariableEmbedding): List<ExpEmbedding>
    fun getPostconditions(returnVariable: VariableEmbedding): List<ExpEmbedding>
}

fun FullNamedFunctionSignature.toViperMethod(
    body: Stmt.Seqn?,
    returnVariable: VariableEmbedding,
    position: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) = UserMethod(
    name,
    formalArgs.map { it.toLocalVarDecl() },
    returnVariable.toLocalVarDecl(),
    getPreconditions(returnVariable).pureToViper(),
    getPostconditions(returnVariable).pureToViper(),
    body,
    position,
    info,
    trafos,
)