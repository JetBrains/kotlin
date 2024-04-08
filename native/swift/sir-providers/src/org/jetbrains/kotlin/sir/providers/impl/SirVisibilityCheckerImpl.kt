/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirVisibilityChecker
import org.jetbrains.kotlin.sir.providers.utils.withSirAnalyse

public class SirVisibilityCheckerImpl(
    private val ktAnalysisSession: KtAnalysisSession,
    private val sirSession: SirSession,
) : SirVisibilityChecker {

    override fun KtSymbolWithVisibility.sirVisibility(): SirVisibility = withSirAnalyse(sirSession, ktAnalysisSession) {
        val ktSymbol = this@sirVisibility
        val isConsumable = isPublic() && when (ktSymbol) {
            is KtNamedClassOrObjectSymbol -> {
                ktSymbol.isConsumableBySirBuilder()
            }
            is KtConstructorSymbol -> {
                true
            }
            is KtFunctionSymbol -> {
                SUPPORTED_SYMBOL_ORIGINS.contains(origin)
                        && !ktSymbol.isSuspend
                        && !ktSymbol.isInline
                        && !ktSymbol.isExtension
                        && !ktSymbol.isOperator
            }
            is KtVariableSymbol -> {
                true
            }
            else -> false
        }
        return if (isConsumable) SirVisibility.PUBLIC else SirVisibility.PRIVATE
    }

    context(KtAnalysisSession)
    private fun KtNamedClassOrObjectSymbol.isConsumableBySirBuilder(): Boolean =
        ((classKind == KtClassKind.CLASS && !isData) || classKind == KtClassKind.OBJECT)
                && (superTypes.count() == 1 && superTypes.first().isAny) // Every class has Any as a superclass
                && !isInline
                && modality == Modality.FINAL

    private fun KtSymbolWithVisibility.isPublic(): Boolean = visibility.isPublicAPI
}

private val SUPPORTED_SYMBOL_ORIGINS = setOf(KtSymbolOrigin.SOURCE, KtSymbolOrigin.LIBRARY)
