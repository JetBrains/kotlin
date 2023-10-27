/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

/**
 * Context in which an `ExpEmbedding` can be flattened to an `Exp` and a sequence of `Stmt`s.
 *
 * We do not distinguish between expressions and statements on the Kotlin side, but we do on the Viper side.
 * As such, an `ExpEmbedding` can represent a nested structure that has to be flattened into sequences
 * of statements. We call this process linearization.
 */
interface LinearizationContext : SeqnBuildContext {
    val source: KtSourceElement?

    fun newVar(type: TypeEmbedding): VariableEmbedding

    fun inhaleForThisStatement(assumption: Exp)
    fun withNewScope(action: LinearizationContext.() -> Unit): Stmt.Seqn
    fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R
}

fun <R> LinearizationContext.withNewVar(type: TypeEmbedding, action: (v: VariableEmbedding) -> R): R = action(newVar(type))
