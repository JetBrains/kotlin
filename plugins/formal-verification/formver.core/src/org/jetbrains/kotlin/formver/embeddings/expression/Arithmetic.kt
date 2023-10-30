/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.embeddings.IntTypeEmbedding
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.ast.Exp

sealed interface IntArithmeticExpression : DirectResultExpEmbedding {
    val left: ExpEmbedding
    val right: ExpEmbedding
    override val type
        get() = IntTypeEmbedding
}

data class Add(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithmeticExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.Add(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class Sub(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithmeticExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.Sub(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class Mul(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithmeticExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.Mul(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

// TODO: handle separately, inhale rhs != 0
data class Div(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithmeticExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.Div(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}

data class Mod(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithmeticExpression {
    override fun toViper(ctx: LinearizationContext) = Exp.Mod(left.toViper(ctx), right.toViper(ctx), ctx.source.asPosition)
}
