/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.DefaultTypeClassIds
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.providers.SirVisibilityChecker
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter
import org.jetbrains.kotlin.sir.providers.utils.deprecatedAnnotation
import org.jetbrains.kotlin.sir.providers.utils.isAbstract
import org.jetbrains.kotlin.utils.findIsInstanceAnd

public class SirVisibilityCheckerImpl(
    private val unsupportedDeclarationReporter: UnsupportedDeclarationReporter,
) : SirVisibilityChecker {

    override fun KaDeclarationSymbol.sirVisibility(ktAnalysisSession: KaSession): SirVisibility = with(ktAnalysisSession) {
        val ktSymbol = this@sirVisibility

        val isHidden = ktSymbol.deprecatedAnnotation?.level == DeprecationLevel.HIDDEN

        val isConsumable = isPublic() && when (ktSymbol) {
            is KaNamedClassSymbol -> {
                ktSymbol.isConsumableBySirBuilder(ktAnalysisSession) && !ktSymbol.hasHiddenAncestors(ktAnalysisSession)
            }
            is KaConstructorSymbol -> {
                if ((ktSymbol.containingSymbol as? KaClassSymbol)?.modality?.isAbstract() != false) {
                    // Hide abstract class constructors from users, but not from other Swift Export modules.
                    return SirVisibility.PACKAGE
                }

                true
            }
            is KaNamedFunctionSymbol -> {
                ktSymbol.isConsumableBySirBuilder(ktSymbol.containingSymbol as? KaClassSymbol)
            }
            is KaVariableSymbol -> {
                !ktSymbol.hasHiddenAccessors
            }
            is KaTypeAliasSymbol -> ktSymbol.expandedType.fullyExpandedType
                .let {
                    it.isPrimitive || it.isNothingType || it.isVisible(ktAnalysisSession)
                }
            else -> false
        }

        return if (isConsumable && !isHidden) SirVisibility.PUBLIC else SirVisibility.PRIVATE
    }

    private fun KaNamedFunctionSymbol.isConsumableBySirBuilder(parent: KaClassSymbol?): Boolean {
        if (isStatic && parent?.let { isValueOfOnEnum(it) } != true) {
            unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "static functions are not supported yet.")
            return false
        }
        if (origin !in SUPPORTED_SYMBOL_ORIGINS) {
            unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "${origin.name.lowercase()} origin is not supported yet.")
            return false
        }
        if (isSuspend) {
            unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "suspend functions are not supported yet.")
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
            if (
                classKind != KaClassKind.CLASS &&
                classKind != KaClassKind.OBJECT &&
                classKind != KaClassKind.COMPANION_OBJECT &&
                classKind != KaClassKind.ENUM_CLASS &&
                classKind != KaClassKind.INTERFACE
            ) {
                unsupportedDeclarationReporter
                    .report(this@isConsumableBySirBuilder, "${classKind.name.lowercase()} classifiers are not supported yet.")
                return@with false
            }
            if (classKind == KaClassKind.INTERFACE && modality == KaSymbolModality.SEALED) {
                return false
            }
            if (classKind == KaClassKind.ENUM_CLASS) {
                return@with true
            }
            if (superTypes.any { it.symbol.let { it?.classId != DefaultTypeClassIds.ANY && it?.sirVisibility(ktAnalysisSession) != SirVisibility.PUBLIC } }) {
                unsupportedDeclarationReporter
                    .report(this@isConsumableBySirBuilder, "inheritance from non-classes is not supported yet.")
                return@with false
            }
            if (typeParameters.isNotEmpty() || superTypes.any { (it as? KaClassType)?.typeArguments?.isEmpty() == false }) {
                unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "generics are not supported yet.")
                return@with false
            }
            if (classId == DefaultTypeClassIds.ANY) {
                unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "${classId} is not supported yet.")
                return@with false
            }
            if (isInline) {
                unsupportedDeclarationReporter.report(this@isConsumableBySirBuilder, "inline classes are not supported yet.")
                return@with false
            }

            return true
        }

    private fun KaType.isVisible(ktAnalysisSession: KaSession): Boolean = with(ktAnalysisSession) {
        (expandedSymbol as? KaDeclarationSymbol)?.sirVisibility(ktAnalysisSession) == SirVisibility.PUBLIC
    }

    private val KaVariableSymbol.hasHiddenAccessors
        get() = (this as? KaPropertySymbol)?.let {
            it.getter?.deprecatedAnnotation?.level == DeprecationLevel.HIDDEN || it.setter?.deprecatedAnnotation?.level == DeprecationLevel.HIDDEN
        } ?: false

    private fun KaClassSymbol.hasHiddenAncestors(ktAnalysisSession: KaSession): Boolean = with(ktAnalysisSession) {
        generateSequence(this@hasHiddenAncestors) {
            it.superTypes.map { it.symbol }.findIsInstanceAnd<KaClassSymbol> { it.classKind != KaClassKind.INTERFACE }
        }.any {
            it.deprecatedAnnotation?.level.let { it == DeprecationLevel.HIDDEN || it == DeprecationLevel.ERROR }
        }
    }

    @OptIn(KaExperimentalApi::class)
    private fun KaDeclarationSymbol.isPublic(): Boolean = compilerVisibility.isPublicAPI

    private fun KaNamedFunctionSymbol.isValueOfOnEnum(parent: KaClassSymbol): Boolean {
        return isStatic && name == StandardNames.ENUM_VALUE_OF && parent.classKind == KaClassKind.ENUM_CLASS
    }
}

private val SUPPORTED_SYMBOL_ORIGINS = setOf(KaSymbolOrigin.SOURCE, KaSymbolOrigin.LIBRARY)