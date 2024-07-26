/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Exp

interface StmtConversionContext<out RTC : ResultTrackingContext> : MethodConversionContext, SeqnBuildContext, ResultTrackingContext {
    val resultCtx: RTC

    fun convert(stmt: FirStatement): Exp

    fun convertAndCapture(exp: FirExpression) {
        resultCtx.capture(convert(exp), embedType(exp))
    }

    fun newBlock(): StmtConversionContext<RTC>
    fun withoutResult(): StmtConversionContext<NoopResultTracker>
    fun withResult(type: TypeEmbedding): StmtConversionContext<VarResultTrackingContext>

    fun withResult(type: TypeEmbedding, action: StmtConversionContext<VarResultTrackingContext>.() -> Unit): Exp {
        val ctx = withResult(type)
        ctx.action()
        return ctx.resultExp
    }
}
