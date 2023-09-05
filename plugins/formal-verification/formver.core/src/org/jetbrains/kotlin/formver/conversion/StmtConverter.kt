/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Exp

/**
 * Tracks the results of converting a block of statements.
 * Kotlin statements, declarations, and expressions do not map to Viper ones one-to-one.
 * Converting a statement with multiple function calls may require storing the
 * intermediate results, which requires introducing new names.  We thus need a
 * shared context for finding fresh variable names.
 */
class StmtConverter<out RTC : ResultTrackingContext>(
    private val methodCtx: MethodConversionContext,
    private val seqnCtx: SeqnBuildContext,
    private val resultCtxFactory: ResultTrackerFactory<RTC>,
    private val nameResolver: NameResolutionContext = SimpleNameResolver(),
) : StmtConversionContext<RTC>, SeqnBuildContext by seqnCtx, MethodConversionContext by methodCtx, ResultTrackingContext {
    override val resultCtx: RTC
        get() = resultCtxFactory.build(this)

    override fun convert(stmt: FirStatement): Exp = stmt.accept(StmtConversionVisitor, this)
    override fun convertAndStore(exp: FirExpression): Exp.LocalVar {
        val convertedExp = convert(exp)
        return convertedExp as? Exp.LocalVar ?: withResult(embedType(exp)) {
            capture(
                convertedExp,
                methodCtx.embedType(exp)
            )
        } as Exp.LocalVar
    }

    override fun newBlock(): StmtConverter<RTC> = StmtConverter(this, SeqnBuilder(), resultCtxFactory)
    override fun withoutResult(): StmtConversionContext<NoopResultTracker> = StmtConverter(this, this.seqnCtx, NoopResultTrackerFactory)

    override fun withResult(type: TypeEmbedding): StmtConverter<VarResultTrackingContext> {
        val newResultVar = newAnonVar(type)
        addDeclaration(newResultVar.toLocalVarDecl())
        return StmtConverter(this, seqnCtx, VarResultTrackerFactory(newResultVar))
    }

    override fun withInlineResolver(
        inlineFunctionName: MangledName,
        resultVar: VariableEmbedding,
        substitutionParams: Map<MangledName, MangledName>,
    ): StmtConversionContext<RTC> {
        return StmtConverter(
            this,
            seqnCtx,
            resultCtxFactory,
            InlineCallNameResolver(inlineFunctionName, resultVar, substitutionParams)
        )
    }

    override fun getVariableEmbedding(name: MangledName, type: TypeEmbedding): VariableEmbedding =
        VariableEmbedding(nameResolver.resolveName(name), type)

    override fun getReturnVariableEmbedding(): VariableEmbedding =
        if (nameResolver is InlineCallNameResolver) {
            getVariableEmbedding(ReturnVariableName, nameResolver.resultVar.type)
        } else {
            getVariableEmbedding(ReturnVariableName, methodCtx.signature.returnType)
        }


    // We can't implement these members using `by` due to Kotlin shenanigans.
    override val resultExp: Exp
        get() = resultCtx.resultExp

    override fun capture(exp: Exp, expType: TypeEmbedding) = resultCtx.capture(exp, expType)
}
