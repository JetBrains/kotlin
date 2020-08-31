/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.element.builder

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext

internal class FirTowerDataContextCollector {
    private val state: MutableMap<FirElement, FirTowerDataContext> = mutableMapOf()

    fun addStatementContext(statement: FirStatement, context: FirTowerDataContext) {
        state[statement] = context
    }

    fun getContext(statement: FirElement): FirTowerDataContext? {
        this.state[statement]?.let { return it }
        for ((key, value) in this.state) {
            // TODO Rework that
            if (key.psi == statement.psi) {
                return value
            }
        }
        return null
    }
}