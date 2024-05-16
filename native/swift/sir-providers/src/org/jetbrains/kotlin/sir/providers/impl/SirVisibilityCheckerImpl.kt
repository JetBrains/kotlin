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
import org.jetbrains.kotlin.sir.providers.SirVisibilityChecker
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter

public class SirVisibilityCheckerImpl(
    private val unsupportedDeclarationReporter: UnsupportedDeclarationReporter,
) : SirVisibilityChecker {

    override fun KtSymbolWithVisibility.sirVisibility(ktAnalysisSession: KtAnalysisSession): SirVisibility = with(ktAnalysisSession) {
        val ktSymbol = this@sirVisibility
        val isConsumable = isPublic() && when (ktSymbol) {
            is KtNamedClassOrObjectSymbol -> {
                ktSymbol.isConsumableBySirBuilder(ktAnalysisSession)
            }
            is KtConstructorSymbol -> {
                true
            }
            is KtFunctionSymbol -> {
                ktSymbol.isConsumableBySirBuilder()
            }
            is KtVariableSymbol -> {
                true
            }
            is KtTypeAliasSymbol -> {
                val type = ktSymbol.expandedType

                !type.isMarkedNullable && type.fullyExpandedType.isPrimitive ||
                        (type.expandedSymbol as? KtSymbolWithVisibility)
                            ?.sirVisibility(ktAnalysisSession) == SirVisibility.PUBLIC
            }
            else -> false
        }
        return if (isConsumable) SirVisibility.PUBLIC else SirVisibility.PRIVATE
    }

    private fun KtFunctionSymbol.isConsumableBySirBuilder(): Boolean {
        if (origin !in SUPPORTED_SYMBOL_ORIGINS) {
            unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "${origin.name.lowercase()} origin is not supported yet.")
            return false
        }
        if (isSuspend) {
            unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "suspend functions are not supported yet.")
            return false
        }
        if (isExtension) {
            unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "extension functions are not supported yet.")
            return false
        }
        if (isOperator) {
            unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "operators are not supported yet.")
            return false
        }
        if (isInline) {
            unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "inline functions are not supported yet.")
            return false
        }
        return true
    }

    private fun KtNamedClassOrObjectSymbol.isConsumableBySirBuilder(ktAnalysisSession: KtAnalysisSession): Boolean =
        with(ktAnalysisSession) {
            if (!((classKind == KtClassKind.CLASS) || classKind == KtClassKind.OBJECT)) {
                unsupportedDeclarationReporter
                    .report(this@isConsumableBySirBuilder, "${classKind.name.lowercase()} classifiers are not supported yet.")
                return@with false
            }
            if (isData) {
                unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "data classes are not supported yet.")
                return@with false
            }
            if (isInner) {
                unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "inner classes are not supported yet.")
                return@with false
            }
            if (!(superTypes.count() == 1 && superTypes.first().isAny)) {
                unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "inheritance is not supported yet.")
                return@with false
            }
            if (isInline) {
                unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "inline classes are not supported yet.")
                return@with false
            }
            if (modality != Modality.FINAL) {
                unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "non-final classes are not supported yet.")
                return@with false
            }
            return true
        }

    private fun KtSymbolWithVisibility.isPublic(): Boolean = visibility.isPublicAPI
}

private val SUPPORTED_SYMBOL_ORIGINS = setOf(KtSymbolOrigin.SOURCE, KtSymbolOrigin.LIBRARY)
