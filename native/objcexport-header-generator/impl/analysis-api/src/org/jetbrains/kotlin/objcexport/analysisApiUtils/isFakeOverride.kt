/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin

/**
 * @return `true` when the declaration is considered a `fake override`.
 * K2 will differentiate fake-overrides into 'intersection override' and 'substitution override'
 * `false` if the symbol is not a fake override
 *
 * See:
 * - [org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.isReal]
 * - [KtSymbolOrigin.INTERSECTION_OVERRIDE]
 * - [KtSymbolOrigin.SUBSTITUTION_OVERRIDE]
 */
internal val KtSymbol.isFakeOverride: Boolean
    get() {
        val origin = this.origin
        if (origin == KtSymbolOrigin.INTERSECTION_OVERRIDE) return true
        if (origin == KtSymbolOrigin.SUBSTITUTION_OVERRIDE) return true
        return false
    }