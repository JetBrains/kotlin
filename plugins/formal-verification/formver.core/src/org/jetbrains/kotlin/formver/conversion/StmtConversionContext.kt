/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Stmt

interface StmtConversionContext : MethodConversionContext {
    val block: Stmt.Seqn
    fun addStatement(stmt: Stmt)
    fun addDeclaration(declaration: Declaration)
    fun convertAndAppend(stmt: FirStatement)
}