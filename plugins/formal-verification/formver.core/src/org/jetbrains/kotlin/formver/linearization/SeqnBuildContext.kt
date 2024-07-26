/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.linearization

import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Stmt

interface SeqnBuildContext {
    val block: Stmt.Seqn
    fun addStatement(stmt: Stmt)
    fun addDeclaration(declaration: Declaration)
}