/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.embeddings.SourceRole
import org.jetbrains.kotlin.formver.embeddings.asInfo
import org.jetbrains.kotlin.formver.embeddings.buildType
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.ast.Exp

sealed interface BinaryBooleanExpression : OperationBaseExpEmbedding {
    override val type
        get() = buildType { boolean() }
}

data class And(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : BinaryBooleanExpression {
    override val refsOperation
        get() = RuntimeTypeDomain.andBools
}

data class Or(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : BinaryBooleanExpression {
    override val refsOperation = RuntimeTypeDomain.orBools
}

data class Implies(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
    override val sourceRole: SourceRole? = null,
) : BinaryBooleanExpression {
    override val refsOperation = RuntimeTypeDomain.impliesBools
}

data class Not(
    override val inner: ExpEmbedding,
    override val sourceRole: SourceRole? = null
) : UnaryDirectResultExpEmbedding {
    override val type = buildType { boolean() }
    override fun toViper(ctx: LinearizationContext) =
        RuntimeTypeDomain.notBool(inner.toViper(ctx), pos = ctx.source.asPosition, info = sourceRole.asInfo)

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        Exp.Not(inner.toViperBuiltinType(ctx), pos = ctx.source.asPosition, info = sourceRole.asInfo)
}

fun List<ExpEmbedding>.toConjunction(): ExpEmbedding =
    if (isEmpty()) BooleanLit(true)
    else reduce { l, r -> And(l, r) }
