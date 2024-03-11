/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.embeddings.FieldEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.*
import org.jetbrains.kotlin.formver.embeddings.mapNotNullUniqueFields
import org.jetbrains.kotlin.formver.linearization.pureToViper
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.viper.ast.UserMethod

interface FullNamedFunctionSignature : NamedFunctionSignature {
    fun getPreconditions(returnVariable: VariableEmbedding): List<ExpEmbedding>
    fun getPostconditions(returnVariable: VariableEmbedding): List<ExpEmbedding>
    val declarationSource: KtSourceElement?
}

fun FullNamedFunctionSignature.toViperMethod(
    body: Stmt.Seqn?,
    returnVariable: VariableEmbedding,
) = UserMethod(
    name,
    formalArgs.map { it.toLocalVarDecl() },
    returnVariable.toLocalVarDecl(),
    getPreconditions(returnVariable).pureToViper(),
    getPostconditions(returnVariable).pureToViper(),
    body,
    declarationSource.asPosition
)