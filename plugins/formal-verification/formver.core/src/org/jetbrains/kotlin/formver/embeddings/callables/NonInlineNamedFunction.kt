/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.conversion.withResult
import org.jetbrains.kotlin.formver.embeddings.ExpEmbedding

class NonInlineNamedFunction(
    val signature: FullNamedFunctionSignature,
) : CallableEmbedding, FullNamedFunctionSignature by signature {
    override val canThrow: Boolean = true
    override fun insertCallImpl(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        ctx.withResult(returnType) {
            addStatement(toMethodCall(args, resultCtx.resultVar))
            resultExp
        }
}