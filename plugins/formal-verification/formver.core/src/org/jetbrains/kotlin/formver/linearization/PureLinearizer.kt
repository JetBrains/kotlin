/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.linearization

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.debug.print
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

class PureLinearizerMisuseException(val offendingFunction: String) : IllegalStateException(offendingFunction)

/**
 * Linearization context that does not permit generation of statements.
 *
 * There are cases in Viper where we expect our result to be an expression by itself, for example when
 * processing preconditions, postconditions, and invariants. In those cases, generating statements
 * would be an error.
 */
class PureLinearizer(override val source: KtSourceElement?) : LinearizationContext {
    override fun <R> withPosition(newSource: KtSourceElement, action: LinearizationContext.() -> R): R =
        PureLinearizer(newSource).action()

    override fun freshAnonVar(type: TypeEmbedding): Exp.LocalVar {
        throw PureLinearizerMisuseException("newVar")
    }

    override fun asBlock(action: LinearizationContext.() -> Unit): Stmt.Seqn {
        throw PureLinearizerMisuseException("withNewScopeToBlock")
    }

    override fun addStatement(stmt: Stmt) {
        throw PureLinearizerMisuseException("addStatement")
    }

    override fun addDeclaration(decl: Declaration) {
        throw PureLinearizerMisuseException("addDeclaration")
    }

    override val block: Stmt.Seqn
        get() = throw PureLinearizerMisuseException("block")
}

fun ExpEmbedding.pureToViper(source: KtSourceElement? = null): Exp {
    try {
        return toViper(PureLinearizer(source))
    } catch (e: PureLinearizerMisuseException) {
        val msg =
            "PureLinearizer used to convert non-pure ExpEmbedding; operation ${e.offendingFunction} is not supported in a pure context.\nEmbedding debug view:\n${debugTreeView.print()}"
        throw IllegalStateException(msg)
    }
}

fun List<ExpEmbedding>.pureToViper(source: KtSourceElement? = null): List<Exp> = map { it.pureToViper(source) }