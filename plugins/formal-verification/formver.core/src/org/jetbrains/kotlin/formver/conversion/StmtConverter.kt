/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.formver.embeddings.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.FullNamedFunctionSignature
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Label
import org.jetbrains.kotlin.name.Name

/**
 * Tracks the results of converting a block of statements.
 * Kotlin statements, declarations, and expressions do not map to Viper ones one-to-one.
 * Converting a statement with multiple function calls may require storing the
 * intermediate results, which requires introducing new names.  We thus need a
 * shared context for finding fresh variable names.
 */
data class StmtConverter<out RTC : ResultTrackingContext>(
    private val methodCtx: MethodConversionContext,
    private val seqnCtx: SeqnBuildContext,
    private val resultCtxFactory: ResultTrackerFactory<RTC>,
    private val whileIndex: Int = 0,
    override val whenSubject: VariableEmbedding? = null,
    private val scopeDepth: Int,
    private val scopedNames: MutableMap<Name, Int> = mutableMapOf(),
) : StmtConversionContext<RTC>, SeqnBuildContext by seqnCtx, MethodConversionContext by methodCtx, ResultTrackingContext,
    WhileStackContext<RTC> {
    override val resultCtx: RTC
        get() = resultCtxFactory.build(this)

    override fun convert(stmt: FirStatement): ExpEmbedding = stmt.accept(StmtConversionVisitorExceptionWrapper, this)
    override fun convertAndStore(exp: FirExpression): VariableEmbedding =
        when (val convertedExp = convert(exp)) {
            is VariableEmbedding -> convertedExp
            else -> withResult(embedType(exp)) { capture(convertedExp) }
        }

    override fun newBlock(): StmtConverter<RTC> = copy(seqnCtx = SeqnBuilder())
    override fun withoutResult(): StmtConversionContext<NoopResultTracker> =
        StmtConverter(this, this.seqnCtx, NoopResultTrackerFactory, whileIndex, whenSubject, scopeDepth, scopedNames)

    override fun withResult(type: TypeEmbedding): StmtConverter<VarResultTrackingContext> {
        val newResultVar = newAnonVar(type)
        addDeclaration(newResultVar.toLocalVarDecl())
        return StmtConverter(this, seqnCtx, VarResultTrackerFactory(newResultVar), whileIndex, whenSubject, scopeDepth, scopedNames)
    }

    override fun withInlineContext(
        inlineSignature: FullNamedFunctionSignature,
        returnVarName: MangledName,
        substitutionParams: Map<Name, SubstitutionItem>,
    ): StmtConversionContext<RTC> =
        copy(
            methodCtx = InlineMethodConverter(this, inlineSignature, returnVarName, substitutionParams, scopeDepth),
            scopedNames = mutableMapOf()
        )

    override fun withLambdaContext(
        inlineSignature: FullNamedFunctionSignature,
        returnVarName: MangledName,
        substitutionParams: Map<Name, SubstitutionItem>,
        scopedNames: Map<Name, Int>
    ): StmtConversionContext<RTC> =
        copy(
            methodCtx = InlineMethodConverter(this, inlineSignature, returnVarName, substitutionParams, scopeDepth),
            scopedNames = scopedNames.toMutableMap()
        )

    // We can't implement these members using `by` due to Kotlin shenanigans.
    override val resultExp: ExpEmbedding
        get() = resultCtx.resultExp

    override fun capture(exp: ExpEmbedding) = resultCtx.capture(exp)

    override val breakLabel: Label
        get() = Label(BreakLabelName(whileIndex), listOf())

    override val continueLabel: Label
        get() = Label(ContinueLabelName(whileIndex), listOf())

    override fun inNewWhileBlock(action: (StmtConversionContext<RTC>) -> Unit) {
        val freshIndex = newWhileIndex()
        val ctx = copy(whileIndex = freshIndex)
        addDeclaration(ctx.continueLabel.toDecl())
        addStatement(ctx.continueLabel.toStmt())
        action(ctx)
        addDeclaration(ctx.breakLabel.toDecl())
        addStatement(ctx.breakLabel.toStmt())
    }

    override fun withWhenSubject(subject: VariableEmbedding?, action: (StmtConversionContext<RTC>) -> Unit) {
        val ctx = copy(whenSubject = subject)
        action(ctx)
    }

    override fun inNewScope(action: (StmtConversionContext<RTC>) -> ExpEmbedding): ExpEmbedding {
        val ctx = copy(scopeDepth = this.scopeDepth + 1, scopedNames = this.scopedNames.toMutableMap())
        return action(ctx)
    }

    override fun addScopedName(name: Name) {
        scopedNames[name] = scopeDepth
    }

    override fun getScopeDepth(name: Name): Int =
        scopedNames[name] ?: throw IllegalArgumentException("$name not found in scope $scopedNames")

    override fun getScopedNames(): Map<Name, Int> = scopedNames
}
