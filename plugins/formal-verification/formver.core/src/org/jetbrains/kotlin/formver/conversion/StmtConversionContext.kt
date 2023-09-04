/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Exp

interface StmtConversionContext : MethodConversionContext, SeqnBuildContext {
    val resultExpr: Exp
    fun convert(stmt: FirStatement): Exp

    fun convertAndCapture(exp: FirExpression) {
        convert(exp)
    }

    fun newBlock(): StmtConversionContext
    fun newBlockShareResult(): StmtConversionContext = newBlock()
    fun withResult(type: TypeEmbedding): StmtWithResultConversionContext

    fun withResult(type: TypeEmbedding, action: StmtWithResultConversionContext.() -> Unit): Exp {
        val ctx = withResult(type)
        ctx.action()
        return ctx.resultVar.toLocalVar()
    }
}

interface StmtWithResultConversionContext : StmtConversionContext {
    val resultVar: VariableEmbedding
    override val resultExpr: Exp
        get() = resultVar.toLocalVar()

    fun captureResult(exp: Exp, expType: TypeEmbedding)

    override fun convertAndCapture(exp: FirExpression) {
        captureResult(convert(exp), embedType(exp))
    }

    override fun newBlockShareResult(): StmtWithResultConversionContext
}
