/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.viper.domains.UnitDomain

abstract class BaseStmtConverter(
    private val methodCtx: MethodConversionContext,
    private val seqnCtx: SeqnBuildContext,
) : StmtConversionContext, SeqnBuildContext by seqnCtx, MethodConversionContext by methodCtx {
    override fun convert(stmt: FirStatement): Exp = stmt.accept(StmtConversionVisitor, this)
    override fun newBlock(): StmtConverter = StmtConverter(this, SeqnBuilder())
    override fun withResult(type: TypeEmbedding): StmtWithAnonResultConverter {
        val newResultVar = newAnonVar(type)
        addDeclaration(newResultVar.toLocalVarDecl())
        return StmtWithAnonResultConverter(this, seqnCtx, newResultVar)
    }
}

/**
 * Tracks the results of converting a block of statements.
 * Kotlin statements, declarations, and expressions do not map to Viper ones one-to-one.
 * Converting a statement with multiple function calls may require storing the
 * intermediate results, which requires introducing new names.  We thus need a
 * shared context for finding fresh variable names.
 */
class StmtConverter(
    methodCtx: MethodConversionContext,
    seqnCtx: SeqnBuildContext,
) : BaseStmtConverter(methodCtx, seqnCtx) {
    override val resultExpr: Exp = UnitDomain.element
}

class StmtWithAnonResultConverter(
    methodCtx: MethodConversionContext,
    seqnCtx: SeqnBuildContext,
    override val resultVar: VariableEmbedding,
) : BaseStmtConverter(methodCtx, seqnCtx), StmtWithResultConversionContext {
    override fun captureResult(exp: Exp) {
        addStatement(Stmt.assign(resultVar.toLocalVar(), exp.withType(resultVar.viperType)))
    }

    override fun newBlockShareResult(): StmtWithResultConversionContext = StmtWithAnonResultConverter(this, SeqnBuilder(), resultVar)
}