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
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Stmt

sealed interface ExpEmbedding {
    val type: TypeEmbedding

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
}

/**
 * `ExpEmbedding` with default `toViperMaybeStoringIn` implementation.
 *
 * This class typically shouldn't be used directly, but this case is common enough that it warrants a common base interface.
 */
sealed interface DefaultMaybeStoringInExpEmbedding : ExpEmbedding {
    override fun toViperMaybeStoringIn(result: Exp.LocalVar?, ctx: LinearizationContext) {
        if (result != null) toViperStoringIn(result, ctx)
        else toViperUnusedResult(ctx)
    }
}

/**
 * `ExpEmbedding` with default `toViperUnusedResult` implementation.
 */
sealed interface DefaultUnusedResultExpEmbedding : ExpEmbedding {
    override fun toViperUnusedResult(ctx: LinearizationContext) {
        toViper(ctx)
    }
}

/**
 * `ExpEmbedding` that produces a result as an expression.
 *
 * The best possible implementation of `toViperStoringIn` simply assigns to the result; we cannot
 * propagate the variable in any way.
 */
sealed interface DirectResultExpEmbedding : DefaultMaybeStoringInExpEmbedding, DefaultUnusedResultExpEmbedding {
    override fun toViperStoringIn(result: Exp.LocalVar, ctx: LinearizationContext) {
        ctx.addStatement(Stmt.assign(result, toViper(ctx)))
    }
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
 */
sealed interface PureExpEmbedding : DirectResultExpEmbedding {
    fun toViper(source: KtSourceElement? = null): Exp
    override fun toViper(ctx: LinearizationContext): Exp = toViper(ctx.source)
}

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
sealed interface UnitResultExpEmbedding : DirectResultExpEmbedding {
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


data class Is(val exp: ExpEmbedding, val comparisonType: TypeEmbedding) : DirectResultExpEmbedding {
    override val type = BooleanTypeEmbedding

    override fun toViper(ctx: LinearizationContext) =
        TypeDomain.isSubtype(TypeOfDomain.typeOf(exp.toViper(ctx)), comparisonType.runtimeType, pos = ctx.source.asPosition)
}

// TODO: probably casts need to be more flexible when it comes to containing result-less nodes.
data class Cast(val exp: ExpEmbedding, override val type: TypeEmbedding) : DirectResultExpEmbedding {
    override fun toViper(ctx: LinearizationContext) = CastingDomain.cast(exp.toViper(ctx), type, ctx.source)
    override fun ignoringCasts(): ExpEmbedding = exp.ignoringCasts()
    override fun ignoringCastsAndMetaNodes(): ExpEmbedding = exp.ignoringCastsAndMetaNodes()
}

data class FieldAccess(val receiver: ExpEmbedding, val field: FieldEmbedding) : DirectResultExpEmbedding {
    override val type: TypeEmbedding = field.type
    override fun toViper(ctx: LinearizationContext) = Exp.FieldAccess(receiver.toViper(ctx), field.toViper(), ctx.source.asPosition)
}

data class FieldAccessPermissions(val exp: ExpEmbedding, val field: FieldEmbedding, val perm: PermExp) : DirectResultExpEmbedding {
    // We consider access permissions to have type Boolean, though this is a bit questionable.
    override val type: TypeEmbedding = BooleanTypeEmbedding

    override fun toViper(ctx: LinearizationContext): Exp {
        val expViper = exp.toViper(ctx)
        return expViper.fieldAccessPredicate(field.toViper(), perm, ctx.source.asPosition)
    }
}

// Ideally we would use the predicate, but due to the possibility of recursion this is inconvenient at present.
data class PredicateAccessPermissions(val predicateName: MangledName, val args: List<ExpEmbedding>) : DirectResultExpEmbedding {
    override val type: TypeEmbedding = BooleanTypeEmbedding
    override fun toViper(ctx: LinearizationContext): Exp =
        Exp.PredicateAccess(predicateName, args.map { it.toViper(ctx) }, ctx.source.asPosition)
}

data class Assign(val lhs: ExpEmbedding, val rhs: ExpEmbedding) : DirectResultExpEmbedding {
    override val type: TypeEmbedding = lhs.type

    override fun toViper(ctx: LinearizationContext): Exp {
        // TODO: this can be done more efficiently by using `toViperStoringIn` when `lhsViper` is an `Exp.LocalVar`.
        val lhsViper = lhs.toViper(ctx)
        val rhsViper = rhs.toViper(ctx)
        ctx.addStatement(Stmt.assign(lhsViper, rhsViper))
        return lhsViper
    }
}

data class Declare(val variable: VariableEmbedding, val initializer: ExpEmbedding?) : UnitResultExpEmbedding {
    override val type: TypeEmbedding = UnitTypeEmbedding

    override fun toViperSideEffects(ctx: LinearizationContext) {
        ctx.addDeclaration(variable.toLocalVarDecl())
        initializer?.toViperStoringIn(variable.toLocalVarUse(), ctx)
    }
}