/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.domains.InjectionImageFunction
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.ast.Operator
import org.jetbrains.kotlin.formver.viper.ast.EqAny
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.NeAny

sealed interface AnyComparisonExpression : BinaryDirectResultExpEmbedding {
    override val type
        get() = BooleanTypeEmbedding

    val comparisonOperation: Operator

    override fun toViper(ctx: LinearizationContext): Exp =
        RuntimeTypeDomain.boolInjection.toRef(
            toViperBuiltinType(ctx),
            pos = ctx.source.asPosition,
            info = sourceRole.asInfo
        )

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp {
        // this check guarantees that leftExp and rightExp will be of the same Viper type
        if (left.type == right.type) {
            val leftExp = left.toViperBuiltinType(ctx)
            val rightExp = right.toViperBuiltinType(ctx)
            val basicComparison = comparisonOperation(leftExp, rightExp, pos = ctx.source.asPosition, info = sourceRole.asInfo)

            val injection = left.type.injectionOr { return basicComparison }

            // This optimization is included to avoid complex comparisons in form `intToRef(0) == intToRef(1)`.
            // May be useful in some cases, e.g. constructor invariants and loop conditions.
            return if (
                leftExp is Exp.DomainFuncApp &&
                rightExp is Exp.DomainFuncApp &&
                leftExp.function == injection.fromRef &&
                rightExp.function == injection.fromRef
            ) comparisonOperation(leftExp.args[0], rightExp.args[0], pos = ctx.source.asPosition, info = sourceRole.asInfo)
            else basicComparison
        } else {
            return comparisonOperation(left.toViper(ctx), right.toViper(ctx), pos = ctx.source.asPosition, info = sourceRole.asInfo)
        }
    }
}

sealed interface IntComparisonExpression : OperationBaseExpEmbedding {
    override val type
        get() = BooleanTypeEmbedding
}

data class LtCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : IntComparisonExpression {
    override val refsOperation: InjectionImageFunction = RuntimeTypeDomain.ltInts
}

data class LeCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : IntComparisonExpression {
    override val refsOperation: InjectionImageFunction = RuntimeTypeDomain.leInts
}

data class GtCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : IntComparisonExpression {
    override val refsOperation: InjectionImageFunction = RuntimeTypeDomain.gtInts
}

data class GeCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : IntComparisonExpression {
    override val refsOperation: InjectionImageFunction = RuntimeTypeDomain.geInts
}

data class EqCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : AnyComparisonExpression {

    override val comparisonOperation = EqAny
}

data class NeCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : AnyComparisonExpression {

    override val comparisonOperation = NeAny
}

fun ExpEmbedding.notNullCmp(): ExpEmbedding = NeCmp(this, NullLit)

