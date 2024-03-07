/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.kt.nodes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.sir.kt.SirSession

/**
 * Base interface for all nodes that are lazy wrappers around [KtSymbol][org.jetbrains.kotlin.analysis.api.symbols.KtSymbol].
 *
 * Conceptually, these classes are very close to Light Classes from Kotlin/JVM.
 * See [SymbolLightClassBase][org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase] as an example.
 */
public interface SirDeclarationFromSymbol {
    public val ktAnalysisSession: KtAnalysisSession
    public val sirSession: SirSession
}

/**
 * A convenient context provider.
 */
internal inline fun <reified R> SirDeclarationFromSymbol.withTranslationContext(action: context(KtAnalysisSession, SirSession) () -> R): R {
    return action(ktAnalysisSession, sirSession)
}