/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.formver.scala.silicon.ast.Stmt
import viper.silver.ast.Declaration

/**
 * Tracks the results of converting a block of statements.
 * Kotlin statements, declarations, and expressions do not map to Viper ones one-to-one.
 * Converting a statement with multiple function calls may require storing the
 * intermediate results, which requires introducing new names.  We thus need a
 * shared context for finding fresh variable names.
 */
class StmtConversionContext(val methodCtx: MethodConversionContext) {
    val statements: MutableList<Stmt> = mutableListOf()
    val declarations: MutableList<Declaration> = mutableListOf()
    val block = Stmt.Seqn(statements, declarations)

    fun convertAndAppend(stmt: FirStatement) {
        stmt.accept(StmtConversionVisitor(), this)
    }
}