/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

/**
 * Standard context for linearization.
 *
 * Note that the implementation for `SeqnBuilderContext` is not provided directly by the `seqnBuilder` constructor parameter;
 * the class may insert extra statements at various points.
 */
data class Linearizer(
    val state: SharedLinearizationState,
    val seqnBuilder: SeqnBuilder,
    override val source: KtSourceElement?,
) : LinearizationContext {
    override fun freshAnonVar(type: TypeEmbedding): Exp.LocalVar {
        val variable = state.freshVar(type)
        addDeclaration(variable.toLocalVarDecl())
        return variable.toLocalVarUse()
    }

    override fun inhaleForThisStatement(assumption: Exp) {
        state.assumptionTracker.addAssumption(assumption)
    }

    override fun asBlock(action: LinearizationContext.() -> Unit): Stmt.Seqn {
        val newBuilder = SeqnBuilder(source)
        copy(seqnBuilder = newBuilder).action()
        return newBuilder.block
    }

    override fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R =
        copy(source = newSource).action()

    override fun addStatement(stmt: Stmt) {
        state.assumptionTracker.forEachForwards { seqnBuilder.addStatement(Stmt.Inhale(it, source.asPosition)) }
        seqnBuilder.addStatement(stmt)
        state.assumptionTracker.forEachBackwards { seqnBuilder.addStatement(Stmt.Exhale(it, source.asPosition)) }
        state.assumptionTracker.clear()
    }

    override fun addDeclaration(decl: Declaration) {
        seqnBuilder.addDeclaration(decl)
    }

    /**
     * This is a stopgap solution for now. Eventually, we would like to perform on-block-exit operations here,
     * so we will need to make an interface that makes it clearer that the builder cannot be reused after
     * `block` has been called.
     */
    override val block: Stmt.Seqn
        get() = seqnBuilder.block
}