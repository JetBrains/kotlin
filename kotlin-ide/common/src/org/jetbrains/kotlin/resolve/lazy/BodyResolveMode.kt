/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.lazy

import org.jetbrains.kotlin.resolve.BindingTraceFilter

enum class BodyResolveMode(val bindingTraceFilter: BindingTraceFilter, val doControlFlowAnalysis: Boolean, val resolveAdditionals: Boolean = true) {
    // All body statements are analyzed, diagnostics included
    FULL(BindingTraceFilter.ACCEPT_ALL, doControlFlowAnalysis = true),

    // Analyzes only dependent statements, including all declaration statements (difference from PARTIAL_WITH_CFA)
    PARTIAL_FOR_COMPLETION(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = true),

    // Analyzes only dependent statements, diagnostics included
    PARTIAL_WITH_DIAGNOSTICS(BindingTraceFilter.ACCEPT_ALL, doControlFlowAnalysis = true),

    // Analyzes only dependent statements, performs control flow analysis (mostly needed for isUsedAsExpression / AsStatement)
    PARTIAL_WITH_CFA(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = true),

    // Analyzes only dependent statements, including only used declaration statements, does not perform control flow analysis
    PARTIAL(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = false),

    // Resolve mode to resolve only the element itself without the additional elements (annotation resolve would not lead to function resolve or default parameters)
    PARTIAL_NO_ADDITIONAL(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = false, resolveAdditionals = false)
    ;

    fun doesNotLessThan(other: BodyResolveMode): Boolean {
        return this <= other && this.bindingTraceFilter.includesEverythingIn(other.bindingTraceFilter)
    }
}
