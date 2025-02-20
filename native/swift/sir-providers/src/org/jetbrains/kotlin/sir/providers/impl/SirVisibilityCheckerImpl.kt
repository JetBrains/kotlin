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
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirVisibilityChecker
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter
import org.jetbrains.kotlin.sir.providers.utils.deprecatedAnnotation
import org.jetbrains.kotlin.sir.providers.utils.isAbstract
import org.jetbrains.kotlin.sir.util.SirPlatformModule
import org.jetbrains.kotlin.utils.findIsInstanceAnd

public class SirVisibilityCheckerImpl(
    private val sirSession: SirSession,
    private val unsupportedDeclarationReporter: UnsupportedDeclarationReporter,
) : SirVisibilityChecker {

    @OptIn(KaExperimentalApi::class)
    override fun KaDeclarationSymbol.sirVisibility(ktAnalysisSession: KaSession): SirVisibility = with(ktAnalysisSession) {
        val ktSymbol = this@sirVisibility

        with(sirSession) {
            val containingModule = ktSymbol.containingModule.sirModule()
            if (containingModule is SirPlatformModule) {
                // The majority of platform libraries can be mapped onto Xcode SDK modules. However, there are exceptions to this rule.
                // This means that `import $platformLibraryName` would be invalid in Swift. For the sake of simplicity, we skip such declarations.
                return if (setOf("posix", "darwin", "zlib", "objc", "builtin", "CFCGTypes").contains(containingModule.name)) {
                    SirVisibility.PRIVATE
                } else {
                    if (ktSymbol is KaTypeAliasSymbol) SirVisibility.PRIVATE
                    else SirVisibility.PUBLIC
                }
            }
        }
        // We care only about public API.
        if (!ktSymbol.compilerVisibility.isPublicAPI) {
            return SirVisibility.PRIVATE
        }
        // Hidden declarations are, well, hidden.
        if (ktSymbol.deprecatedAnnotation?.level == DeprecationLevel.HIDDEN) {
            return SirVisibility.PRIVATE
        }
        // Context parameters are not supported yet in Swift export, so just skip such declarations.
        if (ktSymbol is KaCallableSymbol && ktSymbol.contextParameters.isNotEmpty()) {
            return SirVisibility.PRIVATE
        }
        val isExported = when (ktSymbol) {
            is KaNamedClassSymbol -> {
                ktSymbol.isExported(ktAnalysisSession) && !ktSymbol.hasHiddenAncestors(ktAnalysisSession)
            }
            is KaConstructorSymbol -> {
                if ((ktSymbol.containingSymbol as? KaClassSymbol)?.modality?.isAbstract() != false) {
                    // Hide abstract class constructors from users, but not from other Swift Export modules.
                    return SirVisibility.PACKAGE
                }
                true
            }
            is KaNamedFunctionSymbol -> {
                ktSymbol.isExported(ktSymbol.containingSymbol as? KaClassSymbol)
            }
            is KaVariableSymbol -> {
                !ktSymbol.hasHiddenAccessors
            }
            is KaTypeAliasSymbol -> ktSymbol.expandedType.fullyExpandedType.let {
                it.isPrimitive || it.isNothingType || it.isFunctionType || it.isVisible(ktAnalysisSession)
            }
            else -> false
        }
        return if (isExported) SirVisibility.PUBLIC else SirVisibility.PRIVATE
    }

    private fun KaNamedFunctionSymbol.isExported(parent: KaClassSymbol?): Boolean {
        if (isStatic && parent?.let { isValueOfOnEnum(it) } != true) {
            unsupportedDeclarationReporter.report(this@isExported, "static functions are not supported yet.")
            return false
        }
        if (origin !in SUPPORTED_SYMBOL_ORIGINS) {
            unsupportedDeclarationReporter.report(this@isExported, "${origin.name.lowercase()} origin is not supported yet.")
            return false
        }
        if (isSuspend) {
            unsupportedDeclarationReporter.report(this@isExported, "suspend functions are not supported yet.")
            return false
        }
        if (isOperator) {
            unsupportedDeclarationReporter.report(this@isExported, "operators are not supported yet.")
            return false
        }
        if (isInline) {
            unsupportedDeclarationReporter.report(this@isExported, "inline functions are not supported yet.")
            return false
        }
        return true
    }

    @OptIn(KaExperimentalApi::class)
    private fun KaNamedClassSymbol.isExported(ktAnalysisSession: KaSession): Boolean = with(ktAnalysisSession) {
        // Any is exported as a KotlinBase class.
        if (classId == DefaultTypeClassIds.ANY) {
            return false
        }
        if (classKind == KaClassKind.ANNOTATION_CLASS || classKind == KaClassKind.ANONYMOUS_OBJECT) {
            return@with false
        }
        if (classKind == KaClassKind.INTERFACE && modality == KaSymbolModality.SEALED) {
            return false
        }
        if (classKind == KaClassKind.ENUM_CLASS) {
            if (superTypes.any { it.symbol?.classId?.asSingleFqName() == FqName("kotlinx.cinterop.CEnum") }) {
                unsupportedDeclarationReporter.report(this@isExported, "C enums are not supported yet.")
                return false
            }
            return true
        }
        if (typeParameters.isNotEmpty() || defaultType.allSupertypes.any { it.symbol?.typeParameters?.isNotEmpty() == true }) {
            unsupportedDeclarationReporter.report(this@isExported, "generics are not supported yet.")
            return false
        }
        if (isInline) {
            unsupportedDeclarationReporter.report(this@isExported, "inline classes are not supported yet.")
            return false
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

    private fun KaNamedFunctionSymbol.isValueOfOnEnum(parent: KaClassSymbol): Boolean {
        return isStatic && name == StandardNames.ENUM_VALUE_OF && parent.classKind == KaClassKind.ENUM_CLASS
    }
}

private val SUPPORTED_SYMBOL_ORIGINS = setOf(KaSymbolOrigin.SOURCE, KaSymbolOrigin.LIBRARY)