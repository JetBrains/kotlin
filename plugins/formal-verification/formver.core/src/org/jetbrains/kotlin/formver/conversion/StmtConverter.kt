/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.withPosition
import org.jetbrains.kotlin.formver.names.BreakLabelName
import org.jetbrains.kotlin.formver.names.ContinueLabelName
import org.jetbrains.kotlin.formver.viper.ast.Label

/**
 * Tracks the results of converting a block of statements.
 * Kotlin statements, declarations, and expressions do not map to Viper ones one-to-one.
 * Converting a statement with multiple function calls may require storing the
 * intermediate results, which requires introducing new names.  We thus need a
 * shared context for finding fresh variable names.
 *
 * NOTE: If you add parameters, be sure to update the `withResultFactory` function!
 */
data class StmtConverter(
    private val methodCtx: MethodConversionContext,
    private val whileIndex: Int = 0,
    override val whenSubject: VariableEmbedding? = null,
    override val checkedSafeCallSubject: ExpEmbedding? = null,
    private val scopeIndex: Int = 0,
    override val activeCatchLabels: List<Label> = listOf(),
) : StmtConversionContext, MethodConversionContext by methodCtx {
    override fun convert(stmt: FirStatement): ExpEmbedding =
        stmt.accept(StmtConversionVisitorExceptionWrapper, this).withPosition(stmt.source)

    override fun <R> withNewScope(action: StmtConversionContext.() -> R): R = withNewScopeImpl(action)

    override fun <R> withMethodCtx(factory: MethodContextFactory, action: StmtConversionContext.() -> R): R =
        copy(methodCtx = factory.create(this, scopeIndex)).withNewScope { action() }

    private fun resolveWhileIndex(targetName: String?) =
        if (targetName != null) {
            resolveLoopIndex(targetName)
        } else {
            whileIndex
        }

    override fun continueLabel(targetName: String?): Label {
        val index = resolveWhileIndex(targetName)
        return Label(ContinueLabelName(index), listOf())
    }

    override fun breakLabel(targetName: String?): Label {
        val index = resolveWhileIndex(targetName)
        return Label(BreakLabelName(index), listOf())
    }

    override fun addLoopName(targetName: String) {
        methodCtx.addLoopIdentifier(targetName, whileIndex)
    }

    override fun <R> withFreshWhile(label: FirLabel?, action: StmtConversionContext.() -> R): R =
        withNewScopeImpl {
            val freshIndex = whileIndexProducer.getFresh()
            val ctx = copy(whileIndex = freshIndex)
            label?.let { ctx.addLoopName(it.name) }
            ctx.action()
        }

    override fun <R> withWhenSubject(subject: VariableEmbedding?, action: StmtConversionContext.() -> R): R =
        copy(whenSubject = subject).action()

    override fun <R> withCheckedSafeCallSubject(subject: ExpEmbedding?, action: StmtConversionContext.() -> R): R =
        copy(checkedSafeCallSubject = subject).action()

    override fun <R> withCatches(
        catches: List<FirCatch>,
        action: StmtConversionContext.(catchBlockListData: CatchBlockListData) -> R,
    ): Pair<CatchBlockListData, R> {
        val newCatchLabels = catches.map { Label(catchLabelNameProducer.getFresh(), listOf()) }
        val exitLabel = Label(tryExitLabelNameProducer.getFresh(), listOf())
        val ctx = copy(activeCatchLabels = activeCatchLabels + newCatchLabels)
        val catchBlockListData =
            CatchBlockListData(exitLabel, newCatchLabels.zip(catches).map { (label, firCatch) -> CatchBlockData(label, firCatch) })
        val result = ctx.action(catchBlockListData)
        return Pair(catchBlockListData, result)
    }

    private fun <R> withNewScopeImpl(action: StmtConverter.() -> R): R {
        val newScopeIndex = scopeIndexProducer.getFresh()
        val inner = copy(scopeIndex = newScopeIndex)
        var result: R? = null
        inner.withScopeImpl(newScopeIndex) { result = inner.action() }
        return result!!
    }
}
