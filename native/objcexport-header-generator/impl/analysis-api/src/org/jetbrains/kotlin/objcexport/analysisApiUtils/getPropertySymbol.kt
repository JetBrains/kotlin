/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.tooling.core.linearClosure

context(KtAnalysisSession)
@OptIn(KaExperimentalApi::class)
internal fun KtPropertyAccessorSymbol.getPropertySymbol(): KtPropertySymbol {
    return this.linearClosure<KtSymbol> { it.containingSymbol }.filterIsInstance<KtPropertySymbol>().firstOrNull()
        ?: error("Missing '${KtPropertySymbol::class} on ${this.render()}")
}
