/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.providers.SirVisibilityChecker
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter

public class SirVisibilityCheckerImpl(
    private val unsupportedDeclarationReporter: UnsupportedDeclarationReporter,
) : SirVisibilityChecker {

    override fun KaDeclarationSymbol.sirVisibility(ktAnalysisSession: KaSession): SirVisibility = with(ktAnalysisSession) {
        val ktSymbol = this@sirVisibility
        val isConsumable = isPublic() && when (ktSymbol) {
            is KaNamedClassSymbol -> {
                ktSymbol.isConsumableBySirBuilder(ktAnalysisSession)
            }
            is KaConstructorSymbol -> {
                true
            }
            is KaNamedFunctionSymbol -> {
                ktSymbol.isConsumableBySirBuilder()
            }
            is KaVariableSymbol -> {
                true
            }
            is KaTypeAliasSymbol -> ktSymbol.expandedType.fullyExpandedType
                .takeIf { !it.isMarkedNullable }
                ?.let {
                    it.isPrimitive || it.isNothingType || it.isVisible(ktAnalysisSession)
                } ?: false
            else -> false
        }
        return if (isConsumable) SirVisibility.PUBLIC else SirVisibility.PRIVATE
    }

    private fun KaNamedFunctionSymbol.isConsumableBySirBuilder(): Boolean {
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

    private fun KaNamedClassSymbol.isConsumableBySirBuilder(ktAnalysisSession: KaSession): Boolean =
        with(ktAnalysisSession) {
            if (!((classKind == KaClassKind.CLASS) || classKind == KaClassKind.OBJECT)) {
                unsupportedDeclarationReporter
                    .report(this@isConsumableBySirBuilder, "${classKind.name.lowercase()} classifiers are not supported yet.")
                return@with false
            }
            if (isData && classKind != KaClassKind.OBJECT) {
                unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "data classes are not supported yet.")
                return@with false
            }
            if (isInner) {
                unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "inner classes are not supported yet.")
                return@with false
            }
            if (!(superTypes.count() == 1 && superTypes.first().isAnyType)) {
                unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "inheritance is not supported yet.")
                return@with false
            }
            if (isInline) {
                unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "inline classes are not supported yet.")
                return@with false
            }
            if (modality == KaSymbolModality.ABSTRACT) {
                unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "abstract classes are not supported yet.")
                return@with false
            }
            if (modality == KaSymbolModality.SEALED) {
                unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "sealed classes are not supported yet.")
                return@with false
            }

            return true
        }

    private fun KaType.isVisible(ktAnalysisSession: KaSession): Boolean = with(ktAnalysisSession) {
        (expandedSymbol as? KaDeclarationSymbol)?.sirVisibility(ktAnalysisSession) == SirVisibility.PUBLIC
    }

    @OptIn(KaExperimentalApi::class)
    private fun KaDeclarationSymbol.isPublic(): Boolean = compilerVisibility.isPublicAPI
}

private val SUPPORTED_SYMBOL_ORIGINS = setOf(KaSymbolOrigin.SOURCE, KaSymbolOrigin.LIBRARY)
