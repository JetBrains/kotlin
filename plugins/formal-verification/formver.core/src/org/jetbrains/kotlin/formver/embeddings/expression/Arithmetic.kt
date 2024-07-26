/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.embeddings.buildType

sealed interface IntArithmeticExpression : OperationBaseExpEmbedding {
    override val type
        get() = buildType { int() }
}

data class Add(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithmeticExpression {
    override val refsOperation = RuntimeTypeDomain.plusInts
}

data class Sub(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithmeticExpression {
    override val refsOperation = RuntimeTypeDomain.minusInts
}

data class Mul(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithmeticExpression {
    override val refsOperation = RuntimeTypeDomain.timesInts
}

// TODO: handle separately, inhale rhs != 0
data class Div(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithmeticExpression {
    override val refsOperation = RuntimeTypeDomain.divInts
}

data class Mod(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithmeticExpression {
    override val refsOperation = RuntimeTypeDomain.remInts
}
