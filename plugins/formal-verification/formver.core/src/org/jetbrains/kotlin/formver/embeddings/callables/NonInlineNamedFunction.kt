/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.conversion.withResult
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Method

class NonInlineNamedFunction(
    val signature: FullNamedFunctionSignature,
    val source: KtSourceElement?,
) : ViperAwareCallableEmbedding, FullNamedFunctionSignature by signature {
    override fun insertCallImpl(
        args: List<ExpEmbedding>,
        ctx: StmtConversionContext<ResultTrackingContext>,
        source: KtSourceElement?,
    ): ExpEmbedding =
        ctx.withResult(returnType) {
            addStatement(toMethodCall(args, resultCtx.resultVar, source.asPosition))
            resultExp
        }

    override fun toViperMethod(): Method = signature.toViperMethod(null, source.asPosition)
}