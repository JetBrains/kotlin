/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.domains.InjectionImageFunction
import org.jetbrains.kotlin.formver.embeddings.asInfo
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.ast.Exp

interface OperationBaseExpEmbedding : BinaryDirectResultExpEmbedding {
    val refsOperation: InjectionImageFunction
    val builtinsOperation
        get() = refsOperation.original

    override fun toViper(ctx: LinearizationContext): Exp {
        return refsOperation(left.toViper(ctx), right.toViper(ctx), pos = ctx.source.asPosition, info = sourceRole.asInfo)
    }

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp {
        return builtinsOperation(
            left.toViperBuiltinType(ctx),
            right.toViperBuiltinType(ctx),
            pos = ctx.source.asPosition,
            info = sourceRole.asInfo
        )
    }
}
