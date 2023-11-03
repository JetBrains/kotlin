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

sealed interface ComparisonExpression : BinaryDirectResultExpEmbedding {
    override val type
        get() = BooleanTypeEmbedding
}

data class LtCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : ComparisonExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.LtCmp(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class LeCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : ComparisonExpression {
    override fun toViper(ctx: LinearizationContext) =
        Exp.LeCmp(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition, sourceRole.asInfo)
}

data class GtCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : ComparisonExpression {
    override fun toViper(ctx: LinearizationContext) =
        Exp.GtCmp(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition, sourceRole.asInfo)
}

data class GeCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : ComparisonExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.GeCmp(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class EqCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : ComparisonExpression {
    override fun toViper(ctx: LinearizationContext) =
        Exp.EqCmp(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition, sourceRole.asInfo)
}

data class NeCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : ComparisonExpression {
    override fun toViper(ctx: LinearizationContext) =
        Exp.NeCmp(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition, sourceRole.asInfo)
}

