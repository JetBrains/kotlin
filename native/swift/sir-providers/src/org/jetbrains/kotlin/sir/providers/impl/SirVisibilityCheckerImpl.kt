/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.providers.SirVisibilityChecker
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter

public class SirVisibilityCheckerImpl(
    private val unsupportedDeclarationReporter: UnsupportedDeclarationReporter,
) : SirVisibilityChecker {

    override fun KaSymbolWithVisibility.sirVisibility(ktAnalysisSession: KaSession): SirVisibility = with(ktAnalysisSession) {
        val ktSymbol = this@sirVisibility
        val isConsumable = isPublic() && when (ktSymbol) {
            is KaNamedClassOrObjectSymbol -> {
                ktSymbol.isConsumableBySirBuilder(ktAnalysisSession)
            }
            is KaConstructorSymbol -> {
                true
            }
            is KaFunctionSymbol -> {
                ktSymbol.isConsumableBySirBuilder()
            }
            is KaVariableSymbol -> {
                true
            }
            is KaTypeAliasSymbol -> {
                val type = ktSymbol.expandedType

                !type.isMarkedNullable && type.fullyExpandedType.isPrimitive ||
                        (type.expandedSymbol as? KaSymbolWithVisibility)
                            ?.sirVisibility(ktAnalysisSession) == SirVisibility.PUBLIC
            }
            else -> false
        }
        return if (isConsumable) SirVisibility.PUBLIC else SirVisibility.PRIVATE
    }

    private fun KaFunctionSymbol.isConsumableBySirBuilder(): Boolean {
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

    private fun KaNamedClassOrObjectSymbol.isConsumableBySirBuilder(ktAnalysisSession: KaSession): Boolean =
        with(ktAnalysisSession) {
            if (!((classKind == KaClassKind.CLASS) || classKind == KaClassKind.OBJECT)) {
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

    private fun KaSymbolWithVisibility.isPublic(): Boolean = visibility.isPublicAPI
}

private val SUPPORTED_SYMBOL_ORIGINS = setOf(KaSymbolOrigin.SOURCE, KaSymbolOrigin.LIBRARY)
