/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Stmt

class SeqnBuilder : SeqnBuildContext {
    private val statements: MutableList<Stmt> = mutableListOf()
    private val declarations: MutableList<Declaration> = mutableListOf()
    override val block = Stmt.Seqn(statements, declarations)

    override fun addStatement(stmt: Stmt) {
        statements.add(stmt)
    }

    override fun addDeclaration(declaration: Declaration) {
        declarations.add(declaration)
    }
}