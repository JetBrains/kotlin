/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.linearization

import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Position
import org.jetbrains.kotlin.formver.viper.ast.Stmt

/**
 * Interface available to modifiers during statement generation.
 */
interface AddStatementContext {
    val position: Position

    // Adds a Viper statement directly to the block.
    fun addImmediateStatement(statement: Stmt)
}

/**
 * Represents a modifier to be applied to a generated Viper statement.
 */
sealed interface StmtModifier {
    fun onEntry(ctx: AddStatementContext)
    fun onExit(ctx: AddStatementContext) {}
}

class InhaleExhaleStmtModifier(private val permission: Exp) : StmtModifier {
    override fun onEntry(ctx: AddStatementContext) {
        ctx.addImmediateStatement(Stmt.Inhale(permission, ctx.position))
    }

    override fun onExit(ctx: AddStatementContext) {
        ctx.addImmediateStatement(Stmt.Exhale(permission, ctx.position))
    }
}