/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.embeddings.BooleanTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.SourceRole
import org.jetbrains.kotlin.formver.embeddings.asInfo
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.ast.Exp

sealed interface BinaryBooleanExpression : BinaryDirectResultExpEmbedding {
    override val type: BooleanTypeEmbedding
        get() = BooleanTypeEmbedding
}

data class And(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : BinaryBooleanExpression {
    override fun toViper(ctx: LinearizationContext) =
        Exp.And(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition, sourceRole.asInfo)
}

data class Or(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : BinaryBooleanExpression {
    override fun toViper(ctx: LinearizationContext) =
        Exp.Or(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition, sourceRole.asInfo)
}

data class Implies(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : BinaryBooleanExpression {
    override fun toViper(ctx: LinearizationContext) =
        Exp.Implies(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition, sourceRole.asInfo)
}

data class Not(
    override val inner: ExpEmbedding,
    override val sourceRole: SourceRole? = null
) : UnaryDirectResultExpEmbedding {
    override val type = BooleanTypeEmbedding
    override fun toViper(ctx: LinearizationContext) = Exp.Not(inner.toViper(ctx), ctx.source.asPosition, sourceRole.asInfo)
}

fun List<ExpEmbedding>.toConjunction(): ExpEmbedding =
    if (isEmpty()) BooleanLit(true)
    else reduce { l, r -> And(l, r) }
