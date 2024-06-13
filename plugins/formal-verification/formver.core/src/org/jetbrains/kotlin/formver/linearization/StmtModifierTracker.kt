/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.linearization


/**
 * Tracks a set of modifications to be applied when adding a statement in a linearization context.
 */
class StmtModifierTracker {
    private val modifiers: MutableList<StmtModifier> = mutableListOf()

    fun add(modifier: StmtModifier) {
        modifiers.add(modifier)
    }

    fun applyOnEntry(ctx: AddStatementContext) {
        for (mod in modifiers) { mod.onEntry(ctx) }
    }

    fun applyOnExit(ctx: AddStatementContext) {
        for (mod in modifiers.reversed()) { mod.onExit(ctx) }
    }
}