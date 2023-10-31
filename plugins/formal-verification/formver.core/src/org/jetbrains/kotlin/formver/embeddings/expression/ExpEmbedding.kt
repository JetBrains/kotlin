/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.domains.CastingDomain
import org.jetbrains.kotlin.formver.domains.TypeDomain
import org.jetbrains.kotlin.formver.domains.TypeOfDomain
import org.jetbrains.kotlin.formver.domains.UnitDomain
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.expression.debug.*
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

sealed interface ExpEmbedding {
    val type: TypeEmbedding

    /**
     * The original Kotlin source's role for the generated expression embedding.
     */
    val sourceRole: SourceRole?
        get() = null

    /**
     * Convert this `ExpEmbedding` into a Viper `Exp`, using the provided context for auxiliary statements and declarations.
     *
     * The `Exp` returned contains the result of the expression.
     */
    fun toViper(ctx: LinearizationContext): Exp

    /**
     * Like `toViper`, but store the result in `result`.
     *
     * `result` must be assignable, i.e. a variable or a field access.
     *
     * This function is intended for cases when having the variable already provides some benefit, e.g. in an if statement.
     */
    fun toViperStoringIn(result: Exp.LocalVar, ctx: LinearizationContext)

    /**
     * Like `toViperStoringIn`, but allow special handling of the case when the result is unused.
     */
    fun toViperMaybeStoringIn(result: Exp.LocalVar?, ctx: LinearizationContext)

    /**
     * Like `toViper`, but assume the result is unused.
     */
    fun toViperUnusedResult(ctx: LinearizationContext)

    fun ignoringCasts(): ExpEmbedding = this

    /**
     * Meta nodes are nodes like `WithPosition`.
     */
    fun ignoringMetaNodes(): ExpEmbedding = this

    // TODO: Come up with a better way to solve the problem these `ignoring` functions solve...
    // Probably either virtual functions or a visitor.
    fun ignoringCastsAndMetaNodes(): ExpEmbedding = this

    val debugTreeView: TreeView
}

/**
 * Default implementation for `toViperStoringIn`, which simply assigns the value to the result variable.
 */
sealed interface DefaultStoringInExpEmbedding : ExpEmbedding {
    override fun toViperStoringIn(result: Exp.LocalVar, ctx: LinearizationContext) {
        ctx.addStatement(Stmt.assign(result, toViper(ctx)))
    }
}

/**
 * Default `toViperMaybeStoringIn` implementation, which uses `StoringIn` if there is a result and `UnusedResult` otherwise.
 */
sealed interface DefaultMaybeStoringInExpEmbedding : ExpEmbedding {
    override fun toViperMaybeStoringIn(result: Exp.LocalVar?, ctx: LinearizationContext) {
        if (result != null) toViperStoringIn(result, ctx)
        else toViperUnusedResult(ctx)
    }
}

/**
 * Default `toViperUnusedResult` implementation, that simply uses `toViper`.
 */
sealed interface DefaultUnusedResultExpEmbedding : ExpEmbedding {
    override fun toViperUnusedResult(ctx: LinearizationContext) {
        toViper(ctx)
    }
}

sealed interface OnlyToViperExpEmbedding : DefaultStoringInExpEmbedding, DefaultMaybeStoringInExpEmbedding, DefaultUnusedResultExpEmbedding

/**
 * Default `debugTreeView` implementation that collects trees from a number of possible formats.
 *
 * This covers most use-cases.
 * We don't give `debugAnonymousSubexpressions` a default value since not specifying it explicitly is a good sign we just forgot
 * to implement things for that class.
 */
sealed interface DefaultDebugTreeViewImplementation : ExpEmbedding {
    val debugName: String
        get() = javaClass.simpleName
    val debugAnonymousSubexpressions: List<ExpEmbedding>
    val debugNamedSubexpressions: Map<String, ExpEmbedding>
        get() = mapOf()
    val debugExtraSubtrees: List<TreeView>
        get() = listOf()
    override val debugTreeView: TreeView
        get() {
            val anonymousSubtrees = debugAnonymousSubexpressions.map { it.debugTreeView }
            val namedSubtrees =
                debugNamedSubexpressions.map {
                    DesignatedNode(
                        it.key,
                        it.value.debugTreeView
                    )
                }
            val allSubtrees = anonymousSubtrees + namedSubtrees + debugExtraSubtrees
            return if (allSubtrees.isNotEmpty()) NamedBranchingNode(debugName, allSubtrees)
            else PlaintextLeaf(debugName)
        }
}

/**
 * `ExpEmbedding` that produces a result as an expression.
 *
 * The best possible implementation of `toViperStoringIn` simply assigns to the result; we cannot
 * propagate the variable in any way.
 */
sealed interface DirectResultExpEmbedding : DefaultMaybeStoringInExpEmbedding, DefaultStoringInExpEmbedding,
    DefaultDebugTreeViewImplementation {
    /**
     * When the result is unused, we don't want to produce any expression, but we still want to evaluate the subexpressions.
     */
    val subexpressions: List<ExpEmbedding>

    override fun toViperUnusedResult(ctx: LinearizationContext) {
        for (exp in subexpressions) {
            exp.toViperUnusedResult(ctx)
        }
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = subexpressions
}

sealed interface NullaryDirectResultExpEmbedding : DirectResultExpEmbedding {
    override val subexpressions: List<ExpEmbedding>
        get() = listOf()
}

sealed interface UnaryDirectResultExpEmbedding : DirectResultExpEmbedding {
    val inner: ExpEmbedding

    override val subexpressions: List<ExpEmbedding>
        get() = listOf(inner)
}

sealed interface BinaryDirectResultExpEmbedding : DirectResultExpEmbedding {
    val left: ExpEmbedding
    val right: ExpEmbedding

    override val subexpressions: List<ExpEmbedding>
        get() = listOf(left, right)
}

/**
 * `ExpEmbedding` that requires a location to store its result.
 *
 * The best possible implementation of `toViper` is to generate a fresh location and place the result there.
 */
sealed interface BaseStoredResultExpEmbedding : ExpEmbedding {
    override fun toViper(ctx: LinearizationContext): Exp {
        val variable = ctx.freshAnonVar(type)
        toViperStoringIn(variable, ctx)
        return variable
    }
}

/**
 * `ExpEmbedding` that always produces and stores a result, even if that result is unused.
 */
sealed interface StoredResultExpEmbedding : BaseStoredResultExpEmbedding, DefaultMaybeStoringInExpEmbedding, DefaultUnusedResultExpEmbedding

/**
 * `ExpEmbedding` that does not evaluate to a value, i.e. does not produce any result (not even `Unit`).
 *
 * Examples are `return`, `break`, `continue`...
 */
sealed interface NoResultExpEmbedding : DefaultMaybeStoringInExpEmbedding {
    override val type: TypeEmbedding
        get() = NothingTypeEmbedding

    // Result ignored, since it is never used.
    override fun toViperStoringIn(result: Exp.LocalVar, ctx: LinearizationContext) {
        toViperUnusedResult(ctx)
    }

    override fun toViper(ctx: LinearizationContext): Exp {
        toViperUnusedResult(ctx)
        return UnitDomain.element
    }
}

/**
 * `ExpEmbedding` that can be converted to an `Exp` without any linearization context.
 *
 * Note that such an expression of course cannot have (non-pure) subexpressions, since otherwise they would have to be linearized as well.
 */
sealed interface PureExpEmbedding : NullaryDirectResultExpEmbedding {
    fun toViper(source: KtSourceElement? = null): Exp
    override fun toViper(ctx: LinearizationContext): Exp = toViper(ctx.source)
}

/**
 * `ExpEmbedding` with different behaviour when there is and isn't a result.
 *
 * These are typically control flow structures like `if`.
 */
sealed interface OptionalResultExpEmbedding : BaseStoredResultExpEmbedding {
    override fun toViperUnusedResult(ctx: LinearizationContext) {
        toViperMaybeStoringIn(null, ctx)
    }

    override fun toViperStoringIn(result: Exp.LocalVar, ctx: LinearizationContext) {
        toViperMaybeStoringIn(result, ctx)
    }
}

/**
 * `ExpEmbedding` that wraps another `ExpEmbedding` and delegates all the generation to the inner one.
 *
 * The embedding can still modify the context, which is the main use for this type of embedding.
 */
sealed interface PassthroughExpEmbedding : ExpEmbedding {
    val inner: ExpEmbedding
    override val type: TypeEmbedding
        get() = inner.type

    override fun toViper(ctx: LinearizationContext): Exp = withPassthroughHook(ctx) { inner.toViper(this) }

    override fun toViperStoringIn(result: Exp.LocalVar, ctx: LinearizationContext) {
        withPassthroughHook(ctx) {
            inner.toViperStoringIn(result, this)
        }
    }

    override fun toViperMaybeStoringIn(result: Exp.LocalVar?, ctx: LinearizationContext) {
        withPassthroughHook(ctx) {
            inner.toViperMaybeStoringIn(result, this)
        }
    }

    override fun toViperUnusedResult(ctx: LinearizationContext) {
        withPassthroughHook(ctx) {
            inner.toViperUnusedResult(this)
        }
    }

    fun <R> withPassthroughHook(ctx: LinearizationContext, action: LinearizationContext.() -> R): R
}

/**
 * `ExpEmbedding` that always evaluates to `Unit`.
 */
sealed interface UnitResultExpEmbedding : OnlyToViperExpEmbedding {
    override val type: TypeEmbedding
        get() = UnitTypeEmbedding

    override fun toViper(ctx: LinearizationContext): Exp {
        toViperSideEffects(ctx)
        return UnitDomain.element
    }

    fun toViperSideEffects(ctx: LinearizationContext)
}

fun List<ExpEmbedding>.toViper(ctx: LinearizationContext): List<Exp> = map { it.toViper(ctx) }

fun ExpEmbedding.withType(newType: TypeEmbedding): ExpEmbedding =
    if (newType == type) this else Cast(this, newType)

fun ExpEmbedding.withPosition(source: KtSourceElement?): ExpEmbedding =
    when {
        // Inner position is more specific anyway
        this is WithPosition -> this
        source == null -> this
        else -> WithPosition(this, source)
    }

fun List<ExpEmbedding>.toConjunction(): ExpEmbedding =
    if (isEmpty()) BooleanLit(true)
    else reduce { l, r -> And(l, r) }


data class Is(override val inner: ExpEmbedding, val comparisonType: TypeEmbedding) : UnaryDirectResultExpEmbedding {
    override val type = BooleanTypeEmbedding

    override fun toViper(ctx: LinearizationContext) =
        TypeDomain.isSubtype(TypeOfDomain.typeOf(inner.toViper(ctx)), comparisonType.runtimeType, pos = ctx.source.asPosition)
}

// TODO: probably casts need to be more flexible when it comes to containing result-less nodes.
data class Cast(override val inner: ExpEmbedding, override val type: TypeEmbedding) : UnaryDirectResultExpEmbedding {
    override fun toViper(ctx: LinearizationContext) = CastingDomain.cast(inner.toViper(ctx), type, ctx.source)
    override fun ignoringCasts(): ExpEmbedding = inner.ignoringCasts()
    override fun ignoringCastsAndMetaNodes(): ExpEmbedding = inner.ignoringCastsAndMetaNodes()
}

data class FieldAccess(override val inner: ExpEmbedding, val field: FieldEmbedding) : UnaryDirectResultExpEmbedding {
    override val type: TypeEmbedding = field.type
    override fun toViper(ctx: LinearizationContext) = Exp.FieldAccess(inner.toViper(ctx), field.toViper(), ctx.source.asPosition)

    // field collides with the field context-sensitive keyword.
    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(this.field.debugTreeView)
}

data class FieldAccessPermissions(override val inner: ExpEmbedding, val field: FieldEmbedding, val perm: PermExp) :
    UnaryDirectResultExpEmbedding {
    // We consider access permissions to have type Boolean, though this is a bit questionable.
    override val type: TypeEmbedding = BooleanTypeEmbedding

    override fun toViper(ctx: LinearizationContext): Exp {
        val expViper = inner.toViper(ctx)
        return expViper.fieldAccessPredicate(field.toViper(), perm, ctx.source.asPosition)
    }

    // field collides with the field context-sensitive keyword.
    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(this.field.debugTreeView)
}

// Ideally we would use the predicate, but due to the possibility of recursion this is inconvenient at present.
data class PredicateAccessPermissions(val predicateName: MangledName, val args: List<ExpEmbedding>) : DirectResultExpEmbedding {
    override val type: TypeEmbedding = BooleanTypeEmbedding
    override fun toViper(ctx: LinearizationContext): Exp =
        Exp.PredicateAccess(predicateName, args.map { it.toViper(ctx) }, ctx.source.asPosition)

    override val subexpressions: List<ExpEmbedding>
        get() = args

    override val debugTreeView: TreeView
        get() = NamedBranchingNode("PredicateAccess", buildList {
            add(PlaintextLeaf(predicateName.mangled).withDesignation("name"))
            addAll(args.map { it.debugTreeView })
        })
}

data class Assign(val lhs: ExpEmbedding, val rhs: ExpEmbedding) : UnitResultExpEmbedding, DefaultDebugTreeViewImplementation {
    override val type: TypeEmbedding = lhs.type

    override fun toViperSideEffects(ctx: LinearizationContext) {
        val lhsViper = lhs.toViper(ctx)
        if (lhsViper is Exp.LocalVar) {
            rhs.withType(lhs.type).toViperStoringIn(lhsViper, ctx)
        } else {
            val rhsViper = rhs.withType(lhs.type).toViper(ctx)
            ctx.addStatement(Stmt.assign(lhsViper, rhsViper))
        }
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf(lhs, rhs)
}

data class Declare(val variable: VariableEmbedding, val initializer: ExpEmbedding?) : UnitResultExpEmbedding,
    DefaultDebugTreeViewImplementation {
    override val type: TypeEmbedding = UnitTypeEmbedding

    override fun toViperSideEffects(ctx: LinearizationContext) {
        ctx.addDeclaration(variable.toLocalVarDecl())
        initializer?.toViperStoringIn(variable.toLocalVarUse(), ctx)
    }

    override val debugAnonymousSubexpressions: List<ExpEmbedding>
        get() = listOf()

    override val debugExtraSubtrees: List<TreeView>
        get() = listOfNotNull(variable.debugTreeView, initializer?.debugTreeView)
}