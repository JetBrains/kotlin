/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.domains.*
import org.jetbrains.kotlin.formver.embeddings.callables.DuplicableFunction
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.ast.Exp

sealed interface ExpEmbedding {
    val type: TypeEmbedding

    fun toViper(ctx: LinearizationContext): Exp

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
 * `ExpEmbedding` that can be converted to an `Exp` without any linearization context.
 */
sealed interface PureExpEmbedding : ExpEmbedding {
    fun toViper(source: KtSourceElement? = null): Exp
    override fun toViper(ctx: LinearizationContext): Exp = toViper(ctx.source)
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


    fun <R> withPassthroughHook(ctx: LinearizationContext, action: LinearizationContext.() -> R): R
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

sealed interface IntArithExpression : ExpEmbedding {
    val left: ExpEmbedding
    val right: ExpEmbedding
    override val type
        get() = IntTypeEmbedding
}

data class Add(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.Add(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class Sub(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.Sub(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class Mul(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.Mul(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class Div(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.Div(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class Mod(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.Mod(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}


sealed interface IntComparisonExpression : ExpEmbedding {
    val left: ExpEmbedding
    val right: ExpEmbedding
    override val type
        get() = BooleanTypeEmbedding
}

data class LtCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntComparisonExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.LtCmp(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class LeCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntComparisonExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.LeCmp(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class GtCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntComparisonExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.GtCmp(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class GeCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntComparisonExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.GeCmp(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}


data class EqCmp(
    val left: ExpEmbedding,
    val right: ExpEmbedding,
) : ExpEmbedding {
    override val type = BooleanTypeEmbedding

    override fun toViper(ctx: LinearizationContext) = Exp.EqCmp(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class NeCmp(
    val left: ExpEmbedding,
    val right: ExpEmbedding,
) : ExpEmbedding {
    override val type = BooleanTypeEmbedding

    override fun toViper(ctx: LinearizationContext) = Exp.NeCmp(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}


sealed interface BinaryBooleanExpression : ExpEmbedding {
    val left: ExpEmbedding
    val right: ExpEmbedding
    override val type: BooleanTypeEmbedding
        get() = BooleanTypeEmbedding
}

data class And(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : BinaryBooleanExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.And(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class Or(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : BinaryBooleanExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.Or(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class Implies(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : BinaryBooleanExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.Implies(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class Not(
    val exp: ExpEmbedding,
) : ExpEmbedding {
    override val type = BooleanTypeEmbedding

    override fun toViper(ctx: LinearizationContext) = Exp.Not(exp.toViper(ctx), ctx.source.asPosition)
}

data class Old(
    val exp: ExpEmbedding,
) : ExpEmbedding {
    override val type: TypeEmbedding = exp.type
    override fun toViper(ctx: LinearizationContext): Exp = Exp.Old(exp.toViper(ctx), ctx.source.asPosition)
}

data object UnitLit : ExpEmbedding {
    override val type = UnitTypeEmbedding

    override fun toViper(ctx: LinearizationContext) = UnitDomain.element
}

data class IntLit(val value: Int) : PureExpEmbedding {
    override val type = IntTypeEmbedding

    override fun toViper(source: KtSourceElement?): Exp = Exp.IntLit(value, source.asPosition)
}

data class BooleanLit(val value: Boolean) : PureExpEmbedding {
    override val type = BooleanTypeEmbedding

    override fun toViper(source: KtSourceElement?): Exp = Exp.BoolLit(value, source.asPosition)
}

data class NullLit(val elemType: TypeEmbedding) : PureExpEmbedding {
    override val type = NullableTypeEmbedding(elemType)
    override fun toViper(source: KtSourceElement?): Exp = NullableDomain.nullVal(elemType.viperType, source)
}

data class Is(val exp: ExpEmbedding, val comparisonType: TypeEmbedding) : ExpEmbedding {
    override val type = BooleanTypeEmbedding

    override fun toViper(ctx: LinearizationContext) =
        TypeDomain.isSubtype(TypeOfDomain.typeOf(exp.toViper(ctx)), comparisonType.runtimeType, pos = ctx.source.asPosition)
}

data class Cast(val exp: ExpEmbedding, override val type: TypeEmbedding) : ExpEmbedding {
    override fun toViper(ctx: LinearizationContext) = CastingDomain.cast(exp.toViper(ctx), type, ctx.source)
    override fun ignoringCasts(): ExpEmbedding = exp.ignoringCasts()
    override fun ignoringCastsAndMetaNodes(): ExpEmbedding = exp.ignoringCastsAndMetaNodes()
}

data class FieldAccess(val receiver: ExpEmbedding, val field: FieldEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = field.type
    override fun toViper(ctx: LinearizationContext) = Exp.FieldAccess(receiver.toViper(ctx), field.toViper(), ctx.source.asPosition)
}

data class DuplicableCall(val exp: ExpEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = BooleanTypeEmbedding
    override fun toViper(ctx: LinearizationContext): Exp = DuplicableFunction.toFuncApp(listOf(exp.toViper(ctx)), ctx.source.asPosition)
}

/**
 * Especially when working with type information, there are a number of expressions that do not have a corresponding `ExpEmbedding`.
 * We will eventually want to solve this somehow, but there are still open design questions there, so for now this wrapper will
 * do the job.
 */
data class ExpWrapper(val value: Exp, override val type: TypeEmbedding) : ExpEmbedding {
    override fun toViper(ctx: LinearizationContext): Exp = value
}

data class WithPosition(override val inner: ExpEmbedding, val source: KtSourceElement) : PassthroughExpEmbedding {
    override fun <R> withPassthroughHook(ctx: LinearizationContext, action: LinearizationContext.() -> R): R =
        ctx.withPosition(source, action)

    override fun ignoringMetaNodes(): ExpEmbedding = inner.ignoringMetaNodes()
    override fun ignoringCastsAndMetaNodes(): ExpEmbedding = inner.ignoringCastsAndMetaNodes()
}