/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.expression.debug.TreeView
import org.jetbrains.kotlin.formver.embeddings.expression.debug.debugTreeView
import org.jetbrains.kotlin.formver.embeddings.expression.debug.withDesignation
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.linearization.pureToViper
import org.jetbrains.kotlin.formver.viper.ast.Stmt

data class Is(override val inner: ExpEmbedding, val comparisonType: TypeEmbedding, override val sourceRole: SourceRole? = null) :
    UnaryDirectResultExpEmbedding {
    override val type = buildType { boolean() }

    override fun toViper(ctx: LinearizationContext) =
        RuntimeTypeDomain.boolInjection.toRef(
            RuntimeTypeDomain.isSubtype(
                RuntimeTypeDomain.typeOf(inner.toViper(ctx), pos = ctx.source.asPosition),
                comparisonType.runtimeType,
                pos = ctx.source.asPosition,
                info = sourceRole.asInfo
            ),
            pos = ctx.source.asPosition,
            info = sourceRole.asInfo
        )

    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(comparisonType.debugTreeView.withDesignation("type"))
}


/**
 * ExpEmbedding to change the TypeEmbedding of an inner ExpEmbedding.
 * This is needed since most of our invariants require type and hence can be made more precise via Cast.
 */
data class Cast(override val inner: ExpEmbedding, override val type: TypeEmbedding) : UnaryDirectResultExpEmbedding {
    // TODO: Do we want to assert `inner isOf type` here before making a cast itself?
    override fun toViper(ctx: LinearizationContext) = inner.toViper(ctx)
    override fun ignoringCasts(): ExpEmbedding = inner.ignoringCasts()
    override fun ignoringCastsAndMetaNodes(): ExpEmbedding = inner.ignoringCastsAndMetaNodes()

    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(type.debugTreeView.withDesignation("target"))
}

fun ExpEmbedding.withType(newType: TypeEmbedding): ExpEmbedding = if (type == newType) this else Cast(this, newType)

fun ExpEmbedding.withType(init: TypeBuilder.() -> PretypeBuilder): ExpEmbedding = withType(buildType(init))


/**
 * Implementation of "safe as".
 *
 * We do some special-purpose logic here depending on whether the receiver is nullable or not, hence we cannot use `InhaleProven` directly.
 * This is also why we insist the result is stored; this is a little stronger than necessary, but that does not harm correctness.
 */
data class SafeCast(val exp: ExpEmbedding, val targetType: TypeEmbedding) : StoredResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override val type: NullableTypeEmbedding
        get() = targetType.getNullable()

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        val expViper = exp.toViper(ctx)
        val expWrapped = ExpWrapper(expViper, exp.type)
        val conditional = If(Is(expWrapped, targetType), expWrapped, NullLit, type)
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

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        exp.toViperStoringIn(result, ctx)
        for (invariant in invariants.fillHoles(result)) {
            ctx.addStatement { Stmt.Inhale(invariant.pureToViper(toBuiltin = true, ctx.source), ctx.source.asPosition) }
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
        get() = listOf(type.subTypeInvariant())
}

fun ExpEmbedding.withProvenInvariants(): ExpEmbedding = InhaleProven(this).simplified

class InhaleAccess(exp: ExpEmbedding) : InhaleInvariants(exp) {
    override val invariants: List<TypeInvariantEmbedding>
        get() = type.accessInvariants() + listOfNotNull(type.sharedPredicateAccessInvariant())
}

fun ExpEmbedding.withAccessInvariants(): ExpEmbedding = InhaleAccess(this).simplified

class InhaleAccessAndProven(exp: ExpEmbedding) : InhaleInvariants(exp) {
    override val invariants: List<TypeInvariantEmbedding>
        get() = type.accessInvariants() + listOfNotNull(type.subTypeInvariant(), type.sharedPredicateAccessInvariant())
}

fun ExpEmbedding.withAccessAndProvenInvariants(): ExpEmbedding = InhaleAccessAndProven(this).simplified

fun ExpEmbedding.withNewTypeAccessAndProvenInvariants(newType: TypeEmbedding): ExpEmbedding =
    if (this.type == newType) this
    else InhaleAccessAndProven(this.withType(newType)).simplified
