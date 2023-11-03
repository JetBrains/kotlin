/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.domains.CastingDomain
import org.jetbrains.kotlin.formver.domains.TypeDomain
import org.jetbrains.kotlin.formver.domains.TypeOfDomain
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.expression.debug.TreeView
import org.jetbrains.kotlin.formver.embeddings.expression.debug.debugTreeView
import org.jetbrains.kotlin.formver.embeddings.expression.debug.withDesignation
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.linearization.pureToViper
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

data class Is(override val inner: ExpEmbedding, val comparisonType: TypeEmbedding) : UnaryDirectResultExpEmbedding {
    override val type = BooleanTypeEmbedding

    override fun toViper(ctx: LinearizationContext) =
        TypeDomain.isSubtype(TypeOfDomain.typeOf(inner.toViper(ctx)), comparisonType.runtimeType, pos = ctx.source.asPosition)

    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(comparisonType.debugTreeView.withDesignation("type"))
}

// TODO: probably casts need to be more flexible when it comes to containing result-less nodes.
data class Cast(override val inner: ExpEmbedding, override val type: TypeEmbedding) : UnaryDirectResultExpEmbedding {
    override fun toViper(ctx: LinearizationContext) = CastingDomain.cast(inner.toViper(ctx), type, ctx.source)
    override fun ignoringCasts(): ExpEmbedding = inner.ignoringCasts()
    override fun ignoringCastsAndMetaNodes(): ExpEmbedding = inner.ignoringCastsAndMetaNodes()

    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(type.debugTreeView.withDesignation("target"))
}

fun ExpEmbedding.withType(newType: TypeEmbedding): ExpEmbedding =
    if (newType == type) this else Cast(this, newType)


/**
 * Implementation of "safe as".
 *
 * We do some special-purpose logic here depending on whether the receiver is nullable or not, hence we cannot use `InhaleProven` directly.
 * This is also why we insist the result is stored; this is a little stronger than necessary, but that does not harm correctness.
 */
data class SafeCast(val exp: ExpEmbedding, val targetType: TypeEmbedding) : StoredResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override val type: NullableTypeEmbedding
        get() = targetType.getNullable()

    override fun toViperStoringIn(result: Exp.LocalVar, ctx: LinearizationContext) {
        val expViper = exp.toViper(ctx)
        val expWrapped = ExpWrapper(expViper, exp.type)
        val conditional = If(expWrapped.notNullCmp(), expWrapped, type.nullVal, type)
        conditional.toViperStoringIn(result, ctx)
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(exp)

    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(targetType.debugTreeView.withDesignation("type"))
}

/**
 * Augment this expression with all invariants of a certain kind that we know about the type.
 *
 * This may require storing the result in a variable, if it is not already a variable. The `simplified` property allows
 * unwrapping this type when this can be avoided.
 */
abstract class InhaleInvariants(val exp: ExpEmbedding) : StoredResultExpEmbedding, DefaultDebugTreeViewImplementation {
    // Get the relevant invariants for `type`
    abstract val invariants: List<TypeInvariantEmbedding>

    override val type: TypeEmbedding = exp.type

    override fun toViperStoringIn(result: Exp.LocalVar, ctx: LinearizationContext) {
        exp.toViperStoringIn(result, ctx)
        for (invariant in invariants.fillHoles(ExpWrapper(result, type))) {
            ctx.addStatement(Stmt.Inhale(invariant.pureToViper(), ctx.source.asPosition))
        }
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(exp)

    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(type.debugTreeView.withDesignation("type"))

    val simplified: ExpEmbedding
        get() = if (invariants.isEmpty()) exp
        else this
}

class InhaleProven(exp: ExpEmbedding) : InhaleInvariants(exp) {
    override val invariants: List<TypeInvariantEmbedding>
        get() = type.provenInvariants()
}

fun ExpEmbedding.withProvenInvariants(): ExpEmbedding = InhaleProven(this).simplified

class InhaleAccess(exp: ExpEmbedding) : InhaleInvariants(exp) {
    override val invariants: List<TypeInvariantEmbedding>
        get() = type.accessInvariants()
}

fun ExpEmbedding.withAccessInvariants(): ExpEmbedding = InhaleAccess(this).simplified
