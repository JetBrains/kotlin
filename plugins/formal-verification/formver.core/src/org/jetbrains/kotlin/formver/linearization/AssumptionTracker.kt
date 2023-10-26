/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.linearization

import org.jetbrains.kotlin.formver.viper.ast.Exp

class AssumptionTracker {
    private val assumptions = LinkedHashSet<Exp>()

    fun addAssumption(assumption: Exp) {
        assumptions.add(assumption)
    }

    fun clear() {
        assumptions.clear()
    }

    fun forEachForwards(action: (Exp) -> Unit) = assumptions.forEach(action)
    fun forEachBackwards(action: (Exp) -> Unit) = assumptions.reversed().forEach(action)
}
