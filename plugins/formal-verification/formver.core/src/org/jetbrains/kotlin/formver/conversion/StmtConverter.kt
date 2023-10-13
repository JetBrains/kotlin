/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.formver.embeddings.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.names.BreakLabelName
import org.jetbrains.kotlin.formver.names.ContinueLabelName
import org.jetbrains.kotlin.formver.viper.ast.Label
import org.jetbrains.kotlin.formver.viper.ast.Stmt

/**
 * Tracks the results of converting a block of statements.
 * Kotlin statements, declarations, and expressions do not map to Viper ones one-to-one.
 * Converting a statement with multiple function calls may require storing the
 * intermediate results, which requires introducing new names.  We thus need a
 * shared context for finding fresh variable names.
 *
 * NOTE: If you add parameters, be sure to update the `withResultFactory` function!
 */
data class StmtConverter<out RTC : ResultTrackingContext>(
    private val methodCtx: MethodConversionContext,
    private val seqnCtx: SeqnBuildContext,
    private val resultCtxFactory: ResultTrackerFactory<RTC>,
    private val whileIndex: Int = 0,
    override val whenSubject: VariableEmbedding? = null,
    override val checkedSafeCallSubject: ExpEmbedding? = null,
    private val scopeDepth: Int = 0,
    override val activeCatchLabels: List<Label> = listOf(),
) : StmtConversionContext<RTC>, SeqnBuildContext by seqnCtx, MethodConversionContext by methodCtx, ResultTrackingContext {
    private fun <NewRTC : ResultTrackingContext> withResultFactory(newFactory: ResultTrackerFactory<NewRTC>): StmtConverter<NewRTC> =
        StmtConverter(this, seqnCtx, newFactory, whileIndex, whenSubject, checkedSafeCallSubject, scopeDepth, activeCatchLabels)

    override val resultCtx: RTC
        get() = resultCtxFactory.build(this)

    override fun convert(stmt: FirStatement): ExpEmbedding = stmt.accept(StmtConversionVisitorExceptionWrapper, this)
    override fun store(exp: ExpEmbedding): VariableEmbedding =
        when (exp) {
            is VariableEmbedding -> exp
            else -> withResult(exp.type) { capture(exp) }
        }

    override fun removeResult(): StmtConversionContext<NoopResultTracker> = withResultFactory(NoopResultTrackerFactory)

    override fun addResult(type: TypeEmbedding): StmtConverter<VarResultTrackingContext> {
        val newResultVar = freshAnonVar(type)
        addDeclaration(newResultVar.toLocalVarDecl())
        return withResultFactory(VarResultTrackerFactory(newResultVar))
    }

    override fun withNewScopeToBlock(action: StmtConversionContext<RTC>.() -> Unit): Stmt.Seqn {
        val inner = copy(seqnCtx = SeqnBuilder(), scopeDepth = scopeDepth + 1)
        inner.withScopeImpl(scopeDepth + 1) { inner.action() }
        return inner.block
    }

    override fun <R> withMethodCtx(factory: MethodContextFactory, action: StmtConversionContext<RTC>.() -> R): R =
        copy(methodCtx = factory.create(this, scopeDepth)).withNewScope { action() }

    // We can't implement these members using `by` due to Kotlin shenanigans.
    override val resultExp: ExpEmbedding
        get() = resultCtx.resultExp

    override fun capture(exp: ExpEmbedding) = resultCtx.capture(exp)

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

    override fun <R> withFreshWhile(action: StmtConversionContext<RTC>.() -> R): R {
        val freshIndex = whileIndexProducer.getFresh()
        val ctx = copy(whileIndex = freshIndex)
        addDeclaration(ctx.continueLabel().toDecl())
        addStatement(ctx.continueLabel().toStmt())
        val result = ctx.action()
        addDeclaration(ctx.breakLabel().toDecl())
        addStatement(ctx.breakLabel().toStmt())
        return result
    }

    override fun <R> withWhenSubject(subject: VariableEmbedding?, action: StmtConversionContext<RTC>.() -> R): R =
        copy(whenSubject = subject).action()

    override fun <R> withCheckedSafeCallSubject(subject: ExpEmbedding?, action: StmtConversionContext<RTC>.() -> R): R =
        copy(checkedSafeCallSubject = subject).action()

    override fun withCatches(
        catches: List<FirCatch>,
        action: StmtConversionContext<RTC>.(catchBlockListData: CatchBlockListData) -> Unit,
    ): CatchBlockListData {
        val newCatchLabels = catches.map { Label(catchLabelNameProducer.getFresh(), listOf()) }
        newCatchLabels.forEach { addDeclaration(it.toDecl()) }
        val exitLabel = Label(tryExitLabelNameProducer.getFresh(), listOf())
        addDeclaration(exitLabel.toDecl())
        val ctx = copy(activeCatchLabels = activeCatchLabels + newCatchLabels)
        val catchBlockListData =
            CatchBlockListData(exitLabel, newCatchLabels.zip(catches).map { (label, firCatch) -> CatchBlockData(label, firCatch) })
        ctx.action(catchBlockListData)
        return catchBlockListData
    }
}
