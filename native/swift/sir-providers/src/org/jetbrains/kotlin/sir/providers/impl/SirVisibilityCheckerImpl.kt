/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.DefaultTypeClassIds
import org.jetbrains.kotlin.analysis.api.export.utilities.isClone
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.SirAvailability
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirVisibilityChecker
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter
import org.jetbrains.kotlin.sir.providers.utils.deprecatedAnnotation
import org.jetbrains.kotlin.sir.providers.utils.isAbstract
import org.jetbrains.kotlin.sir.util.SirPlatformModule
import org.jetbrains.kotlin.utils.findIsInstanceAnd
import org.jetbrains.kotlin.analysis.api.export.utilities.*

public class SirVisibilityCheckerImpl(
    private val sirSession: SirSession,
    private val unsupportedDeclarationReporter: UnsupportedDeclarationReporter,
) : SirVisibilityChecker {
    @OptIn(KaExperimentalApi::class)
    override fun KaDeclarationSymbol.sirAvailability(ktAnalysisSession: KaSession): SirAvailability = with(ktAnalysisSession) {
        val ktSymbol = this@sirAvailability

        val visibility = object {
            var value: SirVisibility = SirVisibility.entries.last()
                set(newValue) { field = minOf(field, newValue) }
        }

        with(sirSession) {
            val containingModule = ktSymbol.containingModule.sirModule()
            if (containingModule is SirPlatformModule) {
                // The majority of platform libraries can be mapped onto Xcode SDK modules. However, there are exceptions to this rule.
                // This means that `import $platformLibraryName` would be invalid in Swift. For the sake of simplicity, we skip such declarations.
                if (setOf("posix", "darwin", "zlib", "objc", "builtin", "CFCGTypes").contains(containingModule.name)) {
                    return SirAvailability.Unavailable("Types from certain platform libraries are hidden")
                } else {
                    if (ktSymbol is KaTypeAliasSymbol)
                        return SirAvailability.Hidden("Typealiases from platform libs are sometimes point to custom-exported objc types which we can not detect")
                    else
                        visibility.value = SirVisibility.PUBLIC
                }
            }
        }
        // We care only about public API.
        if (!ktSymbol.compilerVisibility.isPublicAPI) {
            visibility.value = SirVisibility.PRIVATE
        }
        // Hidden declarations are, well, hidden.
        if (ktSymbol.deprecatedAnnotation?.level == DeprecationLevel.HIDDEN) {
            visibility.value = SirVisibility.PRIVATE
        }
        if (ktSymbol is KaCallableSymbol && ktSymbol.contextParameters.isNotEmpty()) {
            return SirAvailability.Unavailable("Callables with context parameters are not supported yet")
        }
        if (containsHidesFromObjCAnnotation(ktSymbol)) {
            return SirAvailability.Unavailable("Declaration is @HiddenFromObjC")
        }
        if ((ktSymbol.containingSymbol as? KaDeclarationSymbol?)?.sirAvailability(ktAnalysisSession) is SirAvailability.Unavailable) {
            return SirAvailability.Unavailable("Declaration's lexical parent is unavailable")
        }
        visibility.value = when (ktSymbol) {
            is KaNamedClassSymbol -> {
                if (!ktSymbol.isExported(ktAnalysisSession) || ktSymbol.hasHiddenAncestors(ktAnalysisSession))
                    return SirAvailability.Unavailable("Type declaration kind isn't supported yet")
                else
                    SirVisibility.PUBLIC
            }
            is KaConstructorSymbol -> {
                if ((ktSymbol.containingSymbol as? KaClassSymbol)?.modality?.isAbstract() != false) {
                    // Hide abstract class constructors from users, but not from other Swift Export modules.
                    SirVisibility.PACKAGE
                } else {
                    SirVisibility.PUBLIC
                }
            }
            is KaNamedFunctionSymbol -> {
                if (!ktSymbol.isExported(ktAnalysisSession)) {
                    return SirAvailability.Hidden("Function declaration kind isn't supported yet")
                } else {
                    SirVisibility.PUBLIC
                }
            }
            is KaVariableSymbol -> {
                if (ktSymbol.hasHiddenAccessors)
                    return SirAvailability.Hidden("Property declaration has hidden accessors")
                else
                    SirVisibility.PUBLIC
            }
            is KaTypeAliasSymbol -> ktSymbol.expandedType.fullyExpandedType.let {
                if (it.isPrimitive || it.isNothingType || it.isFunctionType) {
                    SirVisibility.PUBLIC
                } else when (val availability = it.availability(ktAnalysisSession)) {
                    is SirAvailability.Available -> availability.visibility
                    is SirAvailability.Hidden -> return SirAvailability.Hidden("Typealias target is hidden")
                    is SirAvailability.Unavailable -> return SirAvailability.Unavailable("Typealias target is unavailable")
                }
            }
            else -> return SirAvailability.Unavailable("Declaration kind isn't supported yet")
        }

        return SirAvailability.Available(visibility.value)
    }

    private fun KaNamedFunctionSymbol.isExported(kaSession: KaSession): Boolean = with(kaSession) {
        if (isStatic && !isValueOfOnEnum(this@isExported)) {
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
        if (isClone(this@isExported)) {
            // Cloneable (and its method `clone`) are synthetic on Native, and we don't care about them atm.
            return false
        }
        return true
    }

    @OptIn(KaExperimentalApi::class)
    private fun KaNamedClassSymbol.isExported(ktAnalysisSession: KaSession): Boolean = with(ktAnalysisSession) {

        if (!isAllContainingSymbolsExported(ktAnalysisSession)) {
            return false
        }

        // Any is exported as a KotlinBase class.
        if (classId == DefaultTypeClassIds.ANY) {
            return false
        }
        if (classKind == KaClassKind.ANNOTATION_CLASS || classKind == KaClassKind.ANONYMOUS_OBJECT) {
            return@with false
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
        if (classId?.asSingleFqName()?.startsWith(FqName("kotlin.reflect")) == true) {
            return false
        }
        return true
    }

    private fun KaType.availability(ktAnalysisSession: KaSession): SirAvailability = with(ktAnalysisSession) {
        (expandedSymbol as? KaDeclarationSymbol)?.sirAvailability(ktAnalysisSession) ?: SirAvailability.Unavailable("Type is not a declaration")
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

    private fun KaSession.isValueOfOnEnum(function: KaNamedFunctionSymbol): Boolean {
        with (function) {
            val parent = containingSymbol as? KaClassSymbol ?: return false
            return isStatic && name == StandardNames.ENUM_VALUE_OF && parent.classKind == KaClassKind.ENUM_CLASS
        }
    }

    private fun KaNamedClassSymbol.isAllContainingSymbolsExported(ktAnalysisSession: KaSession): Boolean = with(ktAnalysisSession) {
        if (containingSymbol !is KaNamedClassSymbol) return true
        return (containingSymbol as? KaNamedClassSymbol)?.isExported(ktAnalysisSession) == true
    }
}

private fun KaSession.containsHidesFromObjCAnnotation(symbol: KaAnnotatedSymbol): Boolean {
    return symbol.annotations.any { annotation ->
        val annotationClassId = annotation.classId ?: return@any false
        val annotationClassSymbol = findClass(annotationClassId) ?: return@any false
        ClassId.topLevel(FqName("kotlin.native.HidesFromObjC")) in annotationClassSymbol.annotations
    }
}


private val SUPPORTED_SYMBOL_ORIGINS = setOf(KaSymbolOrigin.SOURCE, KaSymbolOrigin.LIBRARY)
