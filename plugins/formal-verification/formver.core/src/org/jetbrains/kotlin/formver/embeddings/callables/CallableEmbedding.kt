/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.withType

/**
 * Kotlin entity that can be called.
 *
 * Should be used exclusively through `insertCall` below.
 */
interface CallableEmbedding : CallableSignature {
    fun insertCallImpl(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>, source: KtSourceElement?): ExpEmbedding
}

fun CallableEmbedding.insertCall(
    args: List<ExpEmbedding>,
    ctx: StmtConversionContext<ResultTrackingContext>,
    source: KtSourceElement?,
): ExpEmbedding {
    return args.zip(formalArgTypes)
        .map { (arg, type) -> arg.withType(type) }
        .let { insertCallImpl(it, ctx, source) }
        .withType(returnType)
}