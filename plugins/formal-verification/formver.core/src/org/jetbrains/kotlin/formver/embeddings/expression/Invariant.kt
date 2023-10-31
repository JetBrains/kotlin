/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.embeddings.BooleanTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.DuplicableFunction
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.ast.Exp

data class Old(override val inner: ExpEmbedding) : UnaryDirectResultExpEmbedding {
    override val type: TypeEmbedding = inner.type
    override fun toViper(ctx: LinearizationContext): Exp = Exp.Old(inner.toViper(ctx), ctx.source.asPosition)
}

data class DuplicableCall(override val inner: ExpEmbedding) : UnaryDirectResultExpEmbedding {
    override val type: TypeEmbedding = BooleanTypeEmbedding
    override fun toViper(ctx: LinearizationContext): Exp = DuplicableFunction.toFuncApp(listOf(inner.toViper(ctx)), ctx.source.asPosition)
}

